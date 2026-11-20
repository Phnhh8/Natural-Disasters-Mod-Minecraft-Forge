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
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
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

    public static final List ITEMS = new ArrayList();

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
    @SuppressWarnings("unchecked")
    public void registerItems(RegistryEvent.Register<Item> event) {
        Item[] itemArray = (Item[]) ITEMS.toArray(new Item[0]);
        event.getRegistry().registerAll(itemArray);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Object obj : ITEMS) {
            Item item = (Item) obj;
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

