LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
USE ieee.numeric_std.ALL;

ENTITY aes_round IS
    PORT (
        clk : IN STD_LOGIC;
        rst_n : IN STD_LOGIC; -- add this! (active low)
        ena : IN STD_LOGIC; -- pulse or high every round
        round_count : IN UNSIGNED(3 DOWNTO 0); -- 0 to 10
        input_data : IN STD_LOGIC_VECTOR(127 DOWNTO 0); -- plaintext or previous state
        round_key : IN STD_LOGIC_VECTOR(127 DOWNTO 0);
        output_data : OUT STD_LOGIC_VECTOR(127 DOWNTO 0);
        round_done : OUT STD_LOGIC -- one-cycle pulse
    );
END ENTITY;

ARCHITECTURE rtl OF aes_round IS

    SIGNAL sb_out : STD_LOGIC_VECTOR(127 DOWNTO 0);
    SIGNAL sr_out : STD_LOGIC_VECTOR(127 DOWNTO 0);
    SIGNAL mc_out : STD_LOGIC_VECTOR(127 DOWNTO 0);
    SIGNAL round_out : STD_LOGIC_VECTOR(127 DOWNTO 0);

BEGIN

    -- Transformations
    u1 : ENTITY work.subByte PORT MAP (input_data => input_data, output_data => sb_out);
    u2 : ENTITY work.shiftRows PORT MAP (input => sb_out, output => sr_out);
    u3 : ENTITY work.mixColumns PORT MAP (input => sr_out, output => mc_out);

    -- Correct round logic
    round_out <=
        input_data XOR round_key WHEN round_count = 0 ELSE -- initial AddRoundKey
        sr_out XOR round_key WHEN round_count = 10 ELSE -- final round → no MixColumns
        mc_out XOR round_key; -- rounds 1–9

    -- Register output every time we have enable
    PROCESS (clk)
    BEGIN
        IF rising_edge(clk) THEN
            IF ena = '1' THEN
                output_data <= round_out;
                round_done <= '1';
            ELSE
                round_done <= '0';
            END IF;
        END IF;
    END PROCESS;

END rtl;

