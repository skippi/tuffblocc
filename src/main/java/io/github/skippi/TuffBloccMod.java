package io.github.skippi;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Mod(modid = "tuffblocc", name = "tuffblocc", version = "1.0", acceptableRemoteVersions = "*")
public class TuffBloccMod {
  @EventHandler
  public void init(FMLInitializationEvent event) {

    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void substituteExplosion(ExplosionEvent.Detonate event) {
    List<BlockPos> affectedBlocks = event.getAffectedBlocks();
    affectedBlocks.clear();
    affectedBlocks.addAll(computeAffectedBlocks(event.getExplosion(), event.getWorld()));
  }

  private HashSet<BlockPos> computeAffectedBlocks(Explosion explosion, World world) {
    HashSet<BlockPos> result = new HashSet<>();
    for (int j = 0; j < 16; ++j) {
      for (int k = 0; k < 16; ++k) {
        for (int l = 0; l < 16; ++l) {
          if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
            double d0 = ((float) j / 15.0f * 2.0f - 1.0f);
            double d1 = ((float) k / 15.0f * 2.0f - 1.0f);
            double d2 = ((float) l / 15.0f * 2.0f - 1.0f);
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

            d0 /= d3; // d0, d1, d2 unit vector normalized
            d1 /= d3;
            d2 /= d3;

            float size = ObfuscationReflectionHelper
              .getPrivateValue(Explosion.class, explosion, "field_77280_f"); // size
            Vec3d pos = explosion.getPosition();
            double d4 = pos.x;
            double d6 = pos.y;
            double d8 = pos.z;
            float f = size * (0.7f + world.rand.nextFloat() * 0.6f);
            while (f > 0.0f) {
              BlockPos blockpos = new BlockPos(d4, d6, d8);
              IBlockState iblockstate = world.getBlockState(blockpos);

              Entity exploder = ObfuscationReflectionHelper
                .getPrivateValue(Explosion.class, explosion, "field_77283_e"); // exploder

              if (iblockstate.getMaterial() != Material.AIR) {
                float baseResist = Optional.ofNullable(exploder)
                  .map(e -> e.getExplosionResistance(explosion, world, blockpos, iblockstate))
                  .orElse(iblockstate.getBlock().getExplosionResistance(
                    world,
                    blockpos,
                    null,
                    explosion
                  ));

                float antiGriefMultiplier = getAntiGriefMultiplier(iblockstate);
                f -= ((antiGriefMultiplier * baseResist + 0.3f) * 0.3f);
              }

              if (f > 0.0f && (exploder == null || exploder
                .canExplosionDestroyBlock(
                  explosion,
                  world,
                  blockpos,
                  iblockstate,
                  f
                ))) {
                result.add(blockpos);
              }

              d4 += d0 * 0.30000001192092896d;
              d6 += d1 * 0.30000001192092896d;
              d8 += d2 * 0.30000001192092896d;

              f -= 0.22500001f;
            }
          }
        }
      }
    }
    return result;
  }

  private float getAntiGriefMultiplier(IBlockState state) {
    Block block = state.getBlock();
    if (block instanceof BlockDirt || block instanceof BlockGrass || block instanceof BlockClay || block instanceof BlockFalling) {
      return 1.0f;
    } else if (state.getMaterial().blocksMovement()) {
      return 1.0f;
    } else {
      return 2.0f;
    }
  }
}
