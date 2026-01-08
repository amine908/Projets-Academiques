library ieee;
use ieee.STD_LOGIC_1164.all;

entity add_round_key is 
    port ( 
        state_in, subkey_in  : in  std_logic_vector(127 downto 0);
        state_out  : out std_logic_vector(127 downto 0)
    );

end add_round_key;

architecture Beha of add_round_key is 
begin
    state_out <= state_in xor subkey_in;
end Beha;