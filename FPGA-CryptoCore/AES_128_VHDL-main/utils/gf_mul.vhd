library ieee;
use ieee.STD_LOGIC_1164.all;

entity gf_mul is 
    port (
        byte_int : in std_logic_vector(7 downto 0);
        mul2_out: out std_logic_vector(7 downto 0);
        mul3_out: out std_logic_vector(7 downto 0)
    );
end gf_mul;

architecture Behavioral of gf_mul is 

    function mul2 (x : std_logic_vector(7 downto 0)) return std_logic_vector is 
    begin 
        if (x(7) = '0') then 
            return x(6 downto 0) & '0';
        else
            return ((x(6 downto 0) & '0') xor x"1B" );
        end if;
    end mul2;

    function mul3 (x : std_logic_vector(7 downto 0)) return std_logic_vector is 
    begin 
        return mul2(x) xor x;
    end mul3;
begin
    mul2_out <= mul2(byte_int);
    mul3_out <= mul3(byte_int);
     
end Behavioral;