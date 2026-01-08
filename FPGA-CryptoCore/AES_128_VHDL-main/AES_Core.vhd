library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--======================================================================
--  AES_Core
--  Top-level local AES core (controller + key expansion + round block)
--  Interface for the GLOBAL FSM:
--      start   : start one encryption (1 block)
--      key_in  : 128-bit cipher key
--      data_in : 128-bit plaintext
--      data_out: 128-bit ciphertext
--      busy    : core is working
--      done    : one-cycle pulse when ciphertext is ready
--======================================================================
entity AES_Core is
    port (
        clk      : in  std_logic;
        reset    : in  std_logic;                      -- active high
        start    : in  std_logic;                      -- from global FSM
        key_in   : in  std_logic_vector(127 downto 0); -- master key
        data_in  : in  std_logic_vector(127 downto 0); -- plaintext

        data_out : out std_logic_vector(127 downto 0); -- ciphertext
        busy     : out std_logic;
        done     : out std_logic
    );
end entity AES_Core;

architecture rtl of AES_Core is

    --------------------------------------------------------------------
    --  Component declarations (your existing blocks)
    --------------------------------------------------------------------
    component KeyExpansion is
        port (
            clk       : in  std_logic;
            reset     : in  std_logic;
            start     : in  std_logic;
            key_in    : in  std_logic_vector(127 downto 0);
            round_key : out std_logic_vector(127 downto 0);
            round     : out integer range 0 to 10;
            valid     : out std_logic;
            done      : out std_logic
        );
    end component;

    component aes_round is
        port (
            clk         : in  std_logic;
            rst_n       : in  std_logic;                -- active low
            ena         : in  std_logic;                -- enable one round
            round_count : in  unsigned(3 downto 0);     -- 0..10
            input_data  : in  std_logic_vector(127 downto 0);
            round_key   : in  std_logic_vector(127 downto 0);
            output_data : out std_logic_vector(127 downto 0);
            round_done  : out std_logic                 -- (we don't use it)
        );
    end component;

    --------------------------------------------------------------------
    --  Local FSM state
    --------------------------------------------------------------------
    type state_t is (IDLE, RUN, DONE_STATE);
    signal state : state_t := IDLE;

    --------------------------------------------------------------------
    --  Internal registers
    --------------------------------------------------------------------
    signal key_reg   : std_logic_vector(127 downto 0) := (others => '0');
    signal data_reg  : std_logic_vector(127 downto 0) := (others => '0');

    -- AES state (output of aes_round, fed back as next input)
    signal state_data : std_logic_vector(127 downto 0) := (others => '0');

    -- Active-low reset for aes_round
    signal rst_n_sig : std_logic;

    --------------------------------------------------------------------
    --  KeyExpansion <-> controller signals
    --------------------------------------------------------------------
    signal ke_start     : std_logic := '0';
    signal ke_round_key : std_logic_vector(127 downto 0);
    signal ke_round_int : integer range 0 to 10;
    signal ke_valid     : std_logic;
    signal ke_done      : std_logic;

    --------------------------------------------------------------------
    --  aes_round <-> controller signals
    --------------------------------------------------------------------
    signal ar_ena        : std_logic;
    signal ar_round_cnt  : unsigned(3 downto 0);
    signal ar_round_done : std_logic;

    -- Mux for aes_round input data
    signal round_input_data : std_logic_vector(127 downto 0);

begin

    --------------------------------------------------------------------
    --  Reset for aes_round (active low)
    --------------------------------------------------------------------
    rst_n_sig <= not reset;

    --------------------------------------------------------------------
    --  KeyExpansion instance
    --------------------------------------------------------------------
    u_keyexp : KeyExpansion
        port map (
            clk       => clk,
            reset     => reset,
            start     => ke_start,
            key_in    => key_reg,
            round_key => ke_round_key,
            round     => ke_round_int,
            valid     => ke_valid,
            done      => ke_done
        );

    --------------------------------------------------------------------
    --  Mux for input_data of aes_round:
    --    round 0  -> use plaintext (data_reg)
    --    rounds 1..10 -> use previous state (state_data)
    --------------------------------------------------------------------
    round_input_data <= data_reg  when ke_round_int = 0 else
                        state_data;

    --------------------------------------------------------------------
    --  Integer (0..10) -> UNSIGNED(3 downto 0) for aes_round.round_count
    --------------------------------------------------------------------
    ar_round_cnt <= to_unsigned(ke_round_int, 4);

    --------------------------------------------------------------------
    --  aes_round instance
    --------------------------------------------------------------------
    u_round : aes_round
        port map (
            clk         => clk,
            rst_n       => rst_n_sig,
            ena         => ar_ena,
            round_count => ar_round_cnt,
            input_data  => round_input_data,
            round_key   => ke_round_key,
            output_data => state_data,      -- feedback state
            round_done  => ar_round_done    -- not used by controller
        );

    --------------------------------------------------------------------
    --  Ciphertext output = current AES state
    --------------------------------------------------------------------
    data_out <= state_data;

    --------------------------------------------------------------------
    --  Enable for aes_round:
    --    one round per valid round_key during RUN state
    --------------------------------------------------------------------
    ar_ena <= '1' when (state = RUN and ke_valid = '1') else '0';

    --------------------------------------------------------------------
    --  Main controller FSM
    --------------------------------------------------------------------
    process(clk)
    begin
        if rising_edge(clk) then
            if reset = '1' then
                state    <= IDLE;
                key_reg  <= (others => '0');
                data_reg <= (others => '0');
                ke_start <= '0';
                busy     <= '0';
                done     <= '0';

            else
                -- defaults every clock
                ke_start <= '0';
                done     <= '0';

                case state is

                    ---------------------------------------------------
                    when IDLE =>
                        busy <= '0';

                        if start = '1' then
                            -- Latch inputs from global FSM
                            key_reg  <= key_in;
                            data_reg <= data_in;

                            -- Start key expansion
                            ke_start <= '1';

                            state <= RUN;
                        end if;

                    ---------------------------------------------------
                    when RUN =>
                        busy <= '1';

                        -- KeyExpansion + aes_round are running.
                        -- ke_done becomes '1' when round 10 key is issued.
                        if ke_done = '1' then
                            state <= DONE_STATE;
                        end if;

                    ---------------------------------------------------
                    when DONE_STATE =>
                        busy <= '0';
                        done <= '1';  -- one-cycle pulse to global FSM

                        -- Wait for global 'start' to go low before next job
                        if start = '0' then
                            state <= IDLE;
                        end if;

                end case;
            end if;
        end if;
    end process;

end architecture rtl;