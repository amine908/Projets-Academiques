library ieee;
use ieee.std_logic_1164.all;

entity ShiftRows is
    port (
        input: in std_logic_vector(127 downto 0);
        output: out std_logic_vector(127 downto 0)
    );
end ShiftRows;

architecture Beha of ShiftRows is
begin 

    -- to find index we do , 127 - 8xi | i is number of element in martix 
    -- | b0  b4  b8   b12 |
    -- | b1  b5  b9   b13 |
    -- | b2  b6  b10  b14 |
    -- | b3  b7  b11  b15 |

    -- row 1
    output(127 downto 120) <= input(127 downto 120);
    output(95 downto 88) <= input(95 downto 88);
    output(63 downto 56) <= input(63 downto 56);
    output(31 downto 24) <= input(31 downto 24);

    -- row 2
    output(119 downto 112) <= input(87 downto 80);
    output(87 downto 80) <= input(55 downto 48);
    output(55 downto 48) <= input(23 downto 16);
    output(23 downto 16) <= input(119 downto 112);

    -- row 3
    output(111 downto 104) <= input(47 downto 40);
    output(79 downto 72) <= input(15 downto 8);
    output(47 downto 40) <= input(111 downto 104);
    output(15 downto 8) <= input(79 downto 72);

    --row 4
    output(103 downto 96) <= input(7 downto 0);
    output(71 downto 64) <= input(103 downto 96);
    output(39 downto 32) <= input(71 downto 64);
    output(7 downto 0) <= input(39 downto 32);

end Beha;