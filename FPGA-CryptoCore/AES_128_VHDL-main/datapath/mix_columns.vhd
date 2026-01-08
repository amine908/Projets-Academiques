LIBRARY ieee;
USE ieee.std_logic_1164.ALL;

ENTITY MixColumns IS
    PORT (
        input : IN STD_LOGIC_VECTOR(127 DOWNTO 0);
        output : OUT STD_LOGIC_VECTOR(127 DOWNTO 0)
    );
END MixColumns;

ARCHITECTURE Beha OF MixColumns IS
    COMPONENT Column_Calc
        PORT (
            input : IN STD_LOGIC_VECTOR(31 DOWNTO 0);
            output : OUT STD_LOGIC_VECTOR(31 DOWNTO 0)
        );
    END COMPONENT;

BEGIN
    calcul_column_1 : Column_Calc
        PORT MAP(
            input => input(127  DOWNTO 120 - 3*8),
            output => output(127 DOWNTO 120 - 3*8)
        ); 
    calcul_column_2 : Column_Calc
        PORT MAP(
            input => input(127 - 4*8 DOWNTO 120 - 7*8),
            output => output(127 - 4*8 DOWNTO 120 - 7*8)
        );

    calcul_column_3 : Column_Calc
        PORT MAP(
            input => input(127 - 8*8 DOWNTO 120 - 11*8),
            output => output(127 - 8*8 DOWNTO 120 - 11*8)
        );

    calcul_column_4 : Column_Calc
        PORT MAP(
            input => input(127 - 12*8 DOWNTO 120 - 15*8),
            output => output(127 - 12*8 DOWNTO 120 - 15*8)
        );

END Beha;