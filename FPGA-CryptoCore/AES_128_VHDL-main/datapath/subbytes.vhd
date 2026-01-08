LIBRARY ieee;
USE ieee.std_logic_1164.ALL;

ENTITY subbyte IS
	PORT (
		input_data : IN STD_LOGIC_VECTOR(127 DOWNTO 0);
		output_data : OUT STD_LOGIC_VECTOR(127 DOWNTO 0)
	);
END subbyte;

ARCHITECTURE behavioral OF subbyte IS

	COMPONENT sbox
		PORT (
			byte_in : IN STD_LOGIC_VECTOR(7 DOWNTO 0);
			byte_out : OUT STD_LOGIC_VECTOR(7 DOWNTO 0)
		);
	END COMPONENT;
BEGIN
	gen : FOR i IN 0 TO 15 GENERATE
		sbox_inst : sbox
		PORT MAP(
			byte_in => input_data(127 - 8 * i DOWNTO 120 - 8 * i),
			byte_out => output_data(127 - 8 * i DOWNTO 120 - 8 * i)
		);
	END GENERATE gen;

END behavioral;
