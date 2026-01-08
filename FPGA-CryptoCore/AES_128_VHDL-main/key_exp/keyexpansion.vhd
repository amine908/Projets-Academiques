library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity KeyExpansion is
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
end entity KeyExpansion;

architecture Behavioral of KeyExpansion is

    component Sbox is
        port (
            byte_in  : in  std_logic_vector(7 downto 0);
            byte_out : out std_logic_vector(7 downto 0)
        );
    end component;

    type rcon_array is array (1 to 10) of std_logic_vector(31 downto 0);
    constant RCON : rcon_array := (
        x"01000000", x"02000000", x"04000000", x"08000000",
        x"10000000", x"20000000", x"40000000", x"80000000",
        x"1B000000", x"36000000"
    );

    signal w0, w1, w2, w3 : std_logic_vector(31 downto 0);
    signal temp    : std_logic_vector(31 downto 0);
    signal rotated : std_logic_vector(31 downto 0);

    signal sub_byte0, sub_byte1, sub_byte2, sub_byte3 : std_logic_vector(7 downto 0);

    -- ?? Always between 1 and 10 now (safe for RCON index)
    signal current_round : integer range 1 to 10 := 1;
    signal expanding     : std_logic := '0';

begin

    -- RotWord
    rotated <= w3(23 downto 0) & w3(31 downto 24);

    -- S-boxes
    sbox0: Sbox port map (byte_in => rotated(31 downto 24), byte_out => sub_byte0);
    sbox1: Sbox port map (byte_in => rotated(23 downto 16), byte_out => sub_byte1);
    sbox2: Sbox port map (byte_in => rotated(15 downto 8),  byte_out => sub_byte2);
    sbox3: Sbox port map (byte_in => rotated(7 downto 0),   byte_out => sub_byte3);

    -- SubWord(RotWord(w3)) XOR Rcon[current_round]
    temp <= (sub_byte0 & sub_byte1 & sub_byte2 & sub_byte3) xor RCON(current_round);

    process(clk)
    begin
        if rising_edge(clk) then
            if reset = '1' then
                w0            <= (others => '0');
                w1            <= (others => '0');
                w2            <= (others => '0');
                w3            <= (others => '0');
                current_round <= 1;      -- ?? safe index for RCON
                expanding     <= '0';
                valid         <= '0';
                done          <= '0';
                round_key     <= (others => '0');
                round         <= 0;

            else
                -- Default every cycle
                valid <= '0';
                done  <= '0';

                if start = '1' and expanding = '0' then
                    -- Load initial key
                    w0            <= key_in(127 downto 96);
                    w1            <= key_in(95  downto 64);
                    w2            <= key_in(63  downto 32);
                    w3            <= key_in(31  downto 0);

                    current_round <= 1;   -- next round will use RCON(1)
                    expanding     <= '1';

                    -- Output round 0 key
                    round_key <= key_in;
                    round     <= 0;
                    valid     <= '1';

                elsif expanding = '1' then
                    -- Compute new round key (round 1..10)
                    w0 <= w0 xor temp;
                    w1 <= w1 xor (w0 xor temp);
                    w2 <= w2 xor (w1 xor (w0 xor temp));
                    w3 <= w3 xor (w2 xor (w1 xor (w0 xor temp)));

                    round_key <= (w0 xor temp) &
                                 (w1 xor (w0 xor temp)) &
                                 (w2 xor (w1 xor (w0 xor temp))) &
                                 (w3 xor (w2 xor (w1 xor (w0 xor temp))));
                    round     <= current_round;
                    valid     <= '1';

                    if current_round < 10 then
                        current_round <= current_round + 1;
                    else
                        -- current_round = 10: last round key
                        expanding <= '0';
                        done      <= '1';
                    end if;
                end if;
            end if;
        end if;
    end process;

end architecture Behavioral;
