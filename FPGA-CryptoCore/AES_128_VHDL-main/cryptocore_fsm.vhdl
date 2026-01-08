library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--======================================================================
-- ENTITY: CryptoCore_FSM (Unité de Contrôle Globale)
-- Le Chef d'Orchestre du flux de chiffrement (AES/CSPRNG/FIFO).
--======================================================================
entity CryptoCore_FSM is
    port (
        -- Signaux d'Entrée Standard
        CLK              : in  std_logic;
        RESET            : in  std_logic;

        -- STATUTS (Input de la FSM)
        System_START     : in  std_logic;                               -- Démarrage externe
        Key_Refresh_Request : in std_logic;                            -- Demande de renouvellement de clé
        
        data_ready       : in  std_logic;                               -- Statut FIFO Input (DataPath)
        key_valid        : in  std_logic;                               -- Statut CSPRNG (Clé prête)
        ciphertext_ready : in  std_logic;                               -- Statut AES (Chiffrement terminé)
        output_fifo_not_full : in std_logic;                           -- Statut FIFO Output (DataPath)

        -- COMMANDES (Output de la FSM)
        fsm_keygen_start_o       : out std_logic;                       -- Commande au CSPRNG (Utilisé comme Reset/Start)
        fsm_start_enc_o          : out std_logic;                       -- Commande à l'AES_Engine (DataPath)
        fsm_read_enable_o        : out std_logic;                       -- Commande au FIFO Input (DataPath)
        fsm_write_enable_o       : out std_logic;                       -- Commande au FIFO Output (DataPath)
        reset_counter            : out std_logic;                       -- Commande au Mode Controller (pour CTR)
        
        key_valid_out            : out std_logic                        -- État interne: une clé est-elle chargée?
    );
end entity CryptoCore_FSM;

architecture Behavioral of CryptoCore_FSM is

    -- Définition des Types d'États
    type FSM_STATES is (S_IDLE, S_KEY_GENERATION, S_ENCRYPT, S_OUTPUT_READY);
    signal current_state, next_state : FSM_STATES;
    
    -- État interne pour savoir si une clé est déjà chargée et considérée comme sûre
    signal internal_key_valid : std_logic := '0'; 

begin

    -- Mappage direct de l'état interne
    key_valid_out <= internal_key_valid;

    -- -----------------------------------------------------------
    -- PROCESSUS 1 : Registre d'État (Synchronisé par l'horloge)
    -- Gère current_state, RESET et l'état de la clé.
    -- -----------------------------------------------------------
    STATE_REGISTER : process(CLK, RESET)
    begin
        if RESET = '1' then
            current_state <= S_IDLE;
            internal_key_valid <= '0';
        elsif rising_edge(CLK) then
            current_state <= next_state;

            -- Logique d'état de la clé (mis à jour au cycle suivant après la transition)
            if (current_state = S_KEY_GENERATION) and (key_valid = '1') then
                -- Clé générée et valide
                internal_key_valid <= '1';
            elsif (current_state = S_OUTPUT_READY) and (Key_Refresh_Request = '1') then
                -- Lancement du renouvellement de clé
                internal_key_valid <= '0';
            end if;

        end if;
    end process STATE_REGISTER;
    
    -- -----------------------------------------------------------
    -- PROCESSUS 2 : Logique de Transition (Détermination de next_state)
    -- -----------------------------------------------------------
    NEXT_STATE_LOGIC : process(current_state, System_START, data_ready, key_valid, ciphertext_ready, Key_Refresh_Request, internal_key_valid)
    begin
        -- Défaut : l'état reste le même (auto-boucle)
        next_state <= current_state; 

        case current_state is
            
            -- État 1 : S_IDLE (Repos)
            when S_IDLE =>
                -- Démarrage : Donnée prête ET (System_START ou data_ready)
                if (System_START = '1' or data_ready = '1') then
                    if (internal_key_valid = '0') then
                        -- Pas de clé valide -> Générer la clé
                        next_state <= S_KEY_GENERATION;
                    else
                        -- Clé valide -> Chiffrer directement
                        next_state <= S_ENCRYPT;
                    end if;
                end if;

            -- État 2 : S_KEY_GENERATION (Génération de Clé)
            when S_KEY_GENERATION =>
                -- Transition : Clé prête (signal key_valid du CSPRNG)
                if key_valid = '1' then
                    next_state <= S_ENCRYPT;
                end if;
            
            -- État 3 : S_ENCRYPT (Chiffrement)
            when S_ENCRYPT =>
                -- Transition : Chiffrement terminé (signal ciphertext_ready de l'AES)
                if ciphertext_ready = '1' then
                    next_state <= S_OUTPUT_READY;
                end if;
            
            -- État 4 : S_OUTPUT_READY (Transmission)
            when S_OUTPUT_READY =>
                -- 4a : Renouvellement de clé requis
                if Key_Refresh_Request = '1' then
                    next_state <= S_KEY_GENERATION;
                -- 4b : Chiffrement du bloc suivant (data_ready)
                elsif data_ready = '1' then
                    next_state <= S_ENCRYPT;
                -- 4c : Fin du flux de données
                else
                    next_state <= S_IDLE;
                end if;
            
        end case;
    end process NEXT_STATE_LOGIC;

    -- -----------------------------------------------------------
    -- PROCESSUS 3 : Génération des Signaux de Sortie (Actions)
    -- Gère les pulses de commande (Logique combinatoire)
    -- -----------------------------------------------------------
    OUTPUT_ACTIONS : process(current_state, ciphertext_ready, output_fifo_not_full)
    begin
        -- Valeurs par défaut (désactivation des commandes)
        fsm_keygen_start_o       <= '0';
        fsm_start_enc_o          <= '0';
        fsm_read_enable_o        <= '0';
        fsm_write_enable_o       <= '0';
        reset_counter            <= '0';

        case current_state is
            when S_KEY_GENERATION =>
                -- Commande CSPRNG : Démarrer la génération de clé (via RESET/START)
                fsm_keygen_start_o <= '1'; 
                -- Réinitialisation du compteur CTR lors d'une nouvelle clé
                reset_counter      <= '1';

            when S_ENCRYPT =>
                -- Commande AES : Démarrer le chiffrement
                fsm_start_enc_o <= '1';
                -- Commande FIFO Input : Lire le bloc de données pour le fournir à l'AES
                fsm_read_enable_o <= '1';
                
            when S_OUTPUT_READY =>
                -- Commande FIFO Output : Écrire le bloc chiffré
                -- L'écriture est autorisée SI l'AES vient de terminer ET si le FIFO n'est PAS plein
                if (ciphertext_ready = '1' and output_fifo_not_full = '1') then
                    fsm_write_enable_o <= '1';
                end if;

            when others =>
                null;
        end case;
    end process OUTPUT_ACTIONS;

end architecture Behavioral;
