package com.raphydaphy.breakoutapi.testmod;

import com.raphydaphy.breakoutapi.testmod.network.ClientModPackets;
import net.fabricmc.api.ClientModInitializer;

public class BreakoutAPITestClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientModPackets.register();
  }
}
