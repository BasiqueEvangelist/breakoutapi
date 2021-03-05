package com.raphydaphy.breakoutapi.testmod;

import com.raphydaphy.breakoutapi.testmod.command.ModCommands;
import com.raphydaphy.breakoutapi.testmod.network.ModPackets;
import net.fabricmc.api.ModInitializer;

public class BreakoutAPITest implements ModInitializer {
  @Override
  public void onInitialize() {
    ModCommands.register();
    ModPackets.register();
  }
}
