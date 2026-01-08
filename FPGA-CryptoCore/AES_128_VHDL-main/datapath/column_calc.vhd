LIBRARY ieee;
USE ieee.std_logic_1164.ALL;

ENTITY Column_Calc IS
    PORT (
        input : IN STD_LOGIC_VECTOR(31 DOWNTO 0);
        output : OUT STD_LOGIC_VECTOR(31 DOWNTO 0)
    );
END Column_Calc;

ARCHITECTURE Beha OF Column_Calc IS
    SIGNAL temp0_2 : STD_LOGIC_VECTOR(7 DOWNTO 0);
    SIGNAL temp0_3 : STD_LOGIC_VECTOR(7 DOWNTO 0);

    SIGNAL temp1_2 : STD_LOGIC_VECTOR(7 DOWNTO 0);
    SIGNAL temp1_3 : STD_LOGIC_VECTOR(7 DOWNTO 0);

    SIGNAL temp2_2 : STD_LOGIC_VECTOR(7 DOWNTO 0);
    SIGNAL temp2_3 : STD_LOGIC_VECTOR(7 DOWNTO 0);

    SIGNAL temp3_2 : STD_LOGIC_VECTOR(7 DOWNTO 0);
    SIGNAL temp3_3 : STD_LOGIC_VECTOR(7 DOWNTO 0);

    COMPONENT gf_mul
        PORT (
            byte_int : IN STD_LOGIC_VECTOR(7 DOWNTO 0);
            mul2_out : OUT STD_LOGIC_VECTOR(7 DOWNTO 0);
            mul3_out : OUT STD_LOGIC_VECTOR(7 DOWNTO 0)
        );
    END COMPONENT;

BEGIN

    gf_mul_b0 : gf_mul
    PORT MAP(
        byte_int => input(31 DOWNTO 24),
        mul2_out => temp0_2,
        mul3_out => temp0_3
    );

    gf_mul_b1 : gf_mul
    PORT MAP(
        byte_int => input(31 - 8 DOWNTO 24 - 8),
        mul2_out => temp1_2,
        mul3_out => temp1_3
    );

    gf_mul_b2 : gf_mul
    PORT MAP(
        byte_int => input(31 - 2 * 8 DOWNTO 24 - 8 * 2),
        mul2_out => temp2_2,
        mul3_out => temp2_3
    );

    gf_mul_b3 : gf_mul
    PORT MAP(
        byte_int => input(31 - 3 * 8 DOWNTO 24 - 3 * 8),
        mul2_out => temp3_2,
        mul3_out => temp3_3
    );

    output(31 DOWNTO 24) <= temp0_2 XOR temp1_3 XOR input(31 - 2 * 8 DOWNTO 24 - 8 * 2) XOR input(31 - 8 * 3 DOWNTO 24 - 8 * 3);
    output(31 - 8 DOWNTO 24 - 8) <= input(31 DOWNTO 24) XOR temp1_2 XOR temp2_3 XOR input(31 - 8 * 3 DOWNTO 24 - 8 * 3);
    output(31 - 2 * 8 DOWNTO 24 - 2 * 8) <= input(31 DOWNTO 24) XOR input(31 - 8 DOWNTO 24 - 8) XOR temp2_2 XOR temp3_3;
    output(31 - 3 * 8 DOWNTO 24 - 3 * 8) <= temp0_3 XOR input(31 - 8 DOWNTO 24 - 8) XOR input(31 - 2 * 8 DOWNTO 24 - 8 * 2) XOR temp3_2;
END Beha;