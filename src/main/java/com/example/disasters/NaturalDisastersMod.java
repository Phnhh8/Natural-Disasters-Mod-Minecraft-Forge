package com.example.disasters;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod(modid = NaturalDisastersMod.MODID, name = NaturalDisastersMod.NAME, version = NaturalDisastersMod.VERSION)
public class NaturalDisastersMod {

    public static final String MODID = "naturaldisasters";
    public static final String NAME = "Natural Disasters Mod";
    public static final String VERSION = "1.0";

    public static final CreativeTabs DISASTER_TAB = new CreativeTabs("naturaldisasters") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.FIRE_CHARGE);
        }
    };

    public static final List<Item> ITEMS = new ArrayList<>();

    public static final Item TORNADO_ITEM = new DisasterItem("tornado_spawner", "tornado");
    public static final Item METEOR_ITEM = new DisasterItem("meteor_spawner", "meteor");
    public static final Item EARTHQUAKE_ITEM = new DisasterItem("earthquake_spawner", "earthquake");
    public static final Item VOLCANO_ITEM = new DisasterItem("volcano_spawner", "volcano");
    public static final Item LIGHTNING_STORM_ITEM = new DisasterItem("lightning_spawner", "lightning");

    private static Vec3d tornadoPos = null;
    private static int tornadoTicks = 0;
    private static final Random RAND = new Random();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ITEMS.toArray(new Item[0]));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Item item : ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    public static class DisasterItem extends Item {
        private final String disasterType;

        public DisasterItem(String name, String type) {
            this.disasterType = type;
            this.setTranslationKey(MODID + "." + name);
            this.setRegistryName(MODID, name);
            this.setCreativeTab(DISASTER_TAB);
            this.setMaxStackSize(1);
            ITEMS.add(this);
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!world.isRemote) {
                BlockPos pos = player.getPosition();
                triggerDisaster(world, player, pos, disasterType);
            }
            return new ActionResult<>(EnumResult.SUCCESS, stack);
        }
    }

    private static void triggerDisaster(World world, EntityPlayer player, BlockPos pos, String type) {
        switch (type) {
            case "tornado":
                tornadoPos = new Vec3d(pos.getX() + 10, pos.getY(), pos.getZ() + 10);
                tornadoTicks = 400; // 20 giây
                player.sendMessage(new TextComponentString("§c[Thảm họa] Lốc xoáy đã xuất hiện!"));
                break;

            case "meteor":
                for (int i = 0; i < 8; i++) {
                    double offsetX = (RAND.nextDouble() - 0.5) * 30;
                    double offsetZ = (RAND.nextDouble() - 0.5) * 30;
                    EntityLargeFireball meteor = new EntityLargeFireball(world, pos.getX() + offsetX, pos.getY() + 60 + i * 5, pos.getZ() + offsetZ, 0, -1.5, 0);
                    meteor.explosionPower = 6;
                    world.spawnEntity(meteor);
                }
                player.sendMessage(new TextComponentString("§6[Thảm họa] Mưa thiên thạch đang rơi xuống!"));
                break;

            case "earthquake":
                for (int x = -15; x <= 15; x++) {
                    for (int z = -15; z <= 15; z++) {
                        if (RAND.nextInt(4) == 0) {
                            for (int y = -5; y <= 3; y++) {
                                BlockPos target = pos.add(x, y, z);
                                if (!world.isAirBlock(target) && world.getBlockState(target).getBlock() != Blocks.BEDROCK) {
                                    world.destroyBlock(target, false);
                                }
                            }
                        }
                    }
                }
                world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 0.5F);
                player.sendMessage(new TextComponentString("§e[Thảm họa] Động đất dữ dội!"));
                break;

            case "volcano":
                // Núi lửa phun trào mạnh: tạo chóp nham thạch lớn và bắn TNT + cầu lửa
                int radius = 12;
                for (int y = 0; y <= radius; y++) {
                    int layerRadius = radius - y;
                    for (int x = -layerRadius; x <= layerRadius; x++) {
                        for (int z = -layerRadius; z <= layerRadius; z++) {
                            if (x * x + z * z <= layerRadius * layerRadius) {
                                BlockPos p = pos.add(x + 15, y, z);
                                if (x * x + z * z <= (layerRadius - 2) * (layerRadius - 2) && y > 2) {
                                    world.setBlockState(p, Blocks.LAVA.getDefaultState());
                                } else {
                                    world.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
                                }
                            }
                        }
                    }
                }
                for (int i = 0; i < 12; i++) {
                    EntityTNTPrimed tnt = new EntityTNTPrimed(world, pos.getX() + 15, pos.getY() + radius + 3, pos.getZ(), player);
                    tnt.motionX = (RAND.nextDouble() - 0.5) * 1.5;
                    tnt.motionY = 1.0 + RAND.nextDouble();
                    tnt.motionZ = (RAND.nextDouble() - 0.5) * 1.5;
                    tnt.setFuse(60);
                    world.spawnEntity(tnt);
                }
                player.sendMessage(new TextComponentString("§4[Thảm họa] Núi lửa phun trào dữ dội!"));
                break;

            case "lightning":
                for (int i = 0; i < 15; i++) {
                    double lx = pos.getX() + (RAND.nextDouble() - 0.5) * 40;
                    double lz = pos.getZ() + (RAND.nextDouble() - 0.5) * 40;
                    BlockPos top = world.getTopSolidOrLiquidBlock(new BlockPos(lx, 0, lz));
                    world.addWeatherEffect(new EntityLightningBolt(world, top.getX(), top.getY(), top.getZ(), false));
                }
                player.sendMessage(new TextComponentString("§b[Thảm họa] Bão sấm sét!"));
                break;
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

        if (tornadoTicks > 0 && tornadoPos != null) {
            tornadoTicks--;
            World world = event.world;

            Vec3d dir = new Vec3d(RAND.nextDouble() - 0.5, 0, RAND.nextDouble() - 0.5).normalize();
            tornadoPos = tornadoPos.add(dir.x * 0.25, 0, dir.z * 0.25);

            BlockPos center = new BlockPos(tornadoPos);
            for (int y = 0; y  entities = world.getEntitiesWithinAABBExcludingEntity(null,
                    new AxisAlignedBB(center.add(-8, -2, -8), center.add(8, 20, 8)));
            for (net.minecraft.entity.Entity entity : entities) {
                entity.motionY = 0.6;
                entity.motionX += (RAND.nextDouble() - 0.5) * 0.8;
                entity.motionZ += (RAND.nextDouble() - 0.5) * 0.8;
                entity.velocityChanged = true;
            }
        }
    }
}
