package com.example.disasters;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.*;

@Mod(modid = "naturaldisasters", name = "Natural Disasters Mod", version = "1.0")
public class NaturalDisastersMod {
    public static final CreativeTabs DISASTER_TAB = new CreativeTabs("naturaldisasters") {
        @Override public ItemStack createIcon() { return new ItemStack(Items.FIRE_CHARGE); }
    };
    public static final List ITEMS = new ArrayList();
    public static final Item TORNADO = new DisasterItem("tornado_spawner", "tornado");
    public static final Item METEOR = new DisasterItem("meteor_spawner", "meteor");
    public static final Item QUAKE = new DisasterItem("earthquake_spawner", "earthquake");
    public static final Item VOLCANO = new DisasterItem("volcano_spawner", "volcano");
    public static final Item STORM = new DisasterItem("lightning_spawner", "lightning");
    private static Vec3d tornadoPos = null;
    private static int tornadoTicks = 0;
    private static final Random RAND = new Random();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent @SuppressWarnings("unchecked")
    public void registerItems(RegistryEvent.Register<Item> e) { e.getRegistry().registerAll((Item[]) ITEMS.toArray(new Item)); }

    @SideOnly(Side.CLIENT) @SubscribeEvent
    public void registerModels(ModelRegistryEvent e) {
        for (Object o : ITEMS) { Item i = (Item) o; ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory")); }
    }

    public static class DisasterItem extends Item {
        private final String type;
        @SuppressWarnings("unchecked")
        public DisasterItem(String name, String type) {
            this.type = type; setTranslationKey("naturaldisasters." + name); setRegistryName("naturaldisasters", name);
            setCreativeTab(DISASTER_TAB); setMaxStackSize(1); ITEMS.add(this);
        }
        @Override
        public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand h) {
            if (!w.isRemote) triggerDisaster(w, p, p.getPosition(), type);
            return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(h));
        }
    }

    private static void triggerDisaster(World w, EntityPlayer p, BlockPos pos, String type) {
        switch (type) {
            case "tornado":
                tornadoPos = new Vec3d(pos.getX() + 10, pos.getY(), pos.getZ() + 10); tornadoTicks = 400;
                p.sendMessage(new TextComponentString("§c[Thảm họa] Lốc xoáy!")); break;
            case "meteor":
                for (int i = 0; i != 8; i++) {
                    EntityLargeFireball m = new EntityLargeFireball(w, pos.getX() + (RAND.nextDouble() - 0.5) * 30, pos.getY() + 60 + i * 5, pos.getZ() + (RAND.nextDouble() - 0.5) * 30, 0, -1.5, 0);
                    m.explosionPower = 6; w.spawnEntity(m);
                }
                p.sendMessage(new TextComponentString("§6[Thảm họa] Mưa thiên thạch!")); break;
            case "earthquake":
                for (int x = -15; x != 16; x++) for (int z = -15; z != 16; z++) if (RAND.nextInt(4) == 0)
                    for (int y = -5; y != 4; y++) {
                        BlockPos t = pos.add(x, y, z);
                        if (!w.isAirBlock(t) && w.getBlockState(t).getBlock() != Blocks.BEDROCK) w.destroyBlock(t, false);
                    }
                w.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 0.5F);
                p.sendMessage(new TextComponentString("§e[Thảm họa] Động đất!")); break;
            case "volcano":
                int r = 12;
                for (int y = 0; y != r + 1; y++) {
                    int lr = r - y;
                    for (int x = -lr; x != lr + 1; x++) for (int z = -lr; z != lr + 1; z++) {
                        int d = x * x + z * z, m = lr * lr, in = (lr - 2) * (lr - 2);
                        if (d == m || m - d > 0) w.setBlockState(pos.add(x + 15, y, z), (lr > 2 && y > 2 && in - d > 0 ? Blocks.LAVA : Blocks.OBSIDIAN).getDefaultState());
                    }
                }
                for (int i = 0; i != 12; i++) {
                    EntityTNTPrimed tnt = new EntityTNTPrimed(w, pos.getX() + 15, pos.getY() + r + 3, pos.getZ(), p);
                    tnt.motionX = (RAND.nextDouble() - 0.5) * 1.5; tnt.motionY = 1.0 + RAND.nextDouble(); tnt.motionZ = (RAND.nextDouble() - 0.5) * 1.5;
                    tnt.setFuse(60); w.spawnEntity(tnt);
                }
                p.sendMessage(new TextComponentString("§4[Thảm họa] Núi lửa phun trào!")); break;
            case "lightning":
                for (int i = 0; i != 15; i++) {
                    BlockPos top = w.getTopSolidOrLiquidBlock(new BlockPos(pos.getX() + (RAND.nextDouble() - 0.5) * 40, 0, pos.getZ() + (RAND.nextDouble() - 0.5) * 40));
                    w.addWeatherEffect(new EntityLightningBolt(w, top.getX(), top.getY(), top.getZ(), false));
                }
                p.sendMessage(new TextComponentString("§b[Thảm họa] Bão sét!")); break;
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.world.isRemote || tornadoTicks == 0 || tornadoPos == null) return;
        tornadoTicks--; World w = e.world;
        Vec3d d = new Vec3d(RAND.nextDouble() - 0.5, 0, RAND.nextDouble() - 0.5).normalize();
        tornadoPos = tornadoPos.add(d.x * 0.25, 0, d.z * 0.25);
        BlockPos c = new BlockPos(tornadoPos);
        for (int y = 0; y != 18; y++) {
            int r = 2 + (y / 4);
            for (int x = -r; x != r + 1; x++) for (int z = -r; z != r + 1; z++) if (RAND.nextInt(15) == 0) {
                BlockPos bp = c.add(x, y, z); Block b = w.getBlockState(bp).getBlock();
                if (b != Blocks.AIR && b != Blocks.BEDROCK) w.setBlockToAir(bp);
            }
        }
        for (Object o : w.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(c.add(-8, -2, -8), c.add(8, 20, 8)))) {
            Entity en = (Entity) o; en.motionY = 0.6; en.motionX += (RAND.nextDouble() - 0.5) * 0.8; en.motionZ += (RAND.nextDouble() - 0.5) * 0.8; en.velocityChanged = true;
        }
    }
}
