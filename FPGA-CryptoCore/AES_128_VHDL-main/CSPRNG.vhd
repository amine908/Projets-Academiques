library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

--======================================================================
--  CSPRNG (Cryptographically Secure Pseudo-Random Number Generator)
--  Uses the AES_Core in Counter (CTR) mode to generate a secure 128-bit key stream.
--======================================================================
entity CSPRNG is
  port (
    clk       : in  std_logic;
    reset     : in  std_logic;
    -- The output random key (Ciphertext from AES)
    key_out   : out std_logic_vector(127 downto 0);
    -- One-cycle pulse indicating a NEW, valid 128-bit key is ready
    key_valid : out std_logic
  );
end entity CSPRNG;

architecture RTL of CSPRNG is

  -- NOTE: This component declaration is copied directly from your AES_Core interface.
  component AES_Core
    port (
      clk      : in  std_logic;
      reset    : in  std_logic;
      start    : in  std_logic;
      key_in   : in  std_logic_vector(127 downto 0);
      data_in  : in  std_logic_vector(127 downto 0);
      data_out : out std_logic_vector(127 downto 0);
      busy     : out std_logic;
      done     : out std_logic
    );
  end component AES_Core;

  -- ----------------------
  -- Constants (The Fixed Secret Seed)
  -- ----------------------
  -- K_CSPRNG: The fixed, SECRET 128-bit seed. This is the source of entropy.
  -- In a real application, this would be generated randomly and kept secure.
  constant K_CSPRNG : std_logic_vector(127 downto 0) := x"4C6A9E2B5F8D1C7A3E0F9B2D8C1A7F5B";

  -- ----------------------
  -- Internal Signals
  -- ----------------------
  -- The 128-bit Counter (The Nonce, or unique input)
  signal Counter          : unsigned(127 downto 0) := (others => '0');

  -- Control and Data Signals for the AES_Core
  signal aes_start_o      : std_logic := '0';    -- Output: Pulse to start the AES core
  signal aes_busy_i       : std_logic;           -- Input: AES is currently working
  signal aes_done_i       : std_logic;           -- Input: AES is done (one-cycle pulse)
  signal aes_output_data  : std_logic_vector(127 downto 0); -- Input: The ciphertext (Generated Key)

begin

  -- ----------------------------------------------------
  -- 1. AES Core Instantiation (The Scrambler)
  -- ----------------------------------------------------
  AES_CSPRNG_Inst : AES_Core
    port map (
      clk      => clk,
      reset    => reset,
      start    => aes_start_o,
      key_in   => K_CSPRNG,                        -- The Fixed Secret Seed
      data_in  => std_logic_vector(Counter),       -- The Incrementing Counter
      data_out => aes_output_data,                 -- The Generated Key
      busy     => aes_busy_i,
      done     => aes_done_i
    );

  -- ----------------------------------------------------
  -- 2. Output Mapping
  -- ----------------------------------------------------
  key_out   <= aes_output_data; -- Output the generated key (ciphertext)
  key_valid <= aes_done_i;      -- The key is valid precisely when AES pulses 'done'

  -- ----------------------------------------------------
  -- 3. Counter and Control Logic (The Timing Mechanism)
  -- ----------------------------------------------------
  -- This process manages the Counter and generates the necessary 'start' pulse
  -- to ensure continuous, collision-free key generation.
  process(clk)
  begin
    if rising_edge(clk) then
      if reset = '1' then
        Counter     <= (others => '0');
        aes_start_o <= '0';
      else
        -- Default: Start is only a one-cycle pulse, so it is cleared every cycle
        aes_start_o <= '0';

        -- Check for key completion
        if aes_done_i = '1' then
          -- Previous key is valid and output.
          -- 1. Increment the counter for the NEXT key
          Counter <= Counter + 1;
          
          -- 2. Pulse 'start' to immediately begin the next encryption run
          aes_start_o <= '1';
        
        -- Initial Start Condition (Kickstart the first run if reset is inactive)
        elsif Counter = to_unsigned(0, 128) and aes_busy_i = '0' then
          aes_start_o <= '1';
        end if;
      end if;
    end if;
  end process;

end architecture RTL;
