library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity tb_sub_byte is
end tb_sub_byte;

architecture sim of tb_sub_byte is

    signal input_data  : std_logic_vector(127 downto 0);
    signal output_data : std_logic_vector(127 downto 0);

begin

    -- Instantiate the sub_byte module
    uut: entity work.subbyte
        port map (
            input_data  => input_data,
            output_data => output_data
        );

    process
    begin
        -- Test vector from AES official example
        input_data <= x"00112233445566778899aabbccddeeff";

        wait for 10 ns;

        -- Expected: 63cab7040953d051cd60e0e7ba70e18c
        assert output_data = x"63cab7040953d051cd60e0e7ba70e18c"
        report "SubBytes output mismatch!" severity error;

        wait;
    end process;

end sim;
