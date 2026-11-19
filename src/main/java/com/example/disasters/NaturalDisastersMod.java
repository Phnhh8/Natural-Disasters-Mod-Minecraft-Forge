package com.example.disasters;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.Random;

@Mod("naturaldisasters")
public class NaturalDisastersMod {
    public static final String MODID = "naturaldisasters";
    private static int tickCounter = 0, disasterDuration = 0, tsunamiWaveX = 0;
    private static String activeDisaster = "NONE";
    private static Vec3 tornadoPos = null;
    private static BlockPos targetPos = null;
    private static final Random RANDOM = new Random();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> METEOR_CALLER = reg("meteor_caller", "METEOR");
    public static final RegistryObject<Item> EARTHQUAKE_CALLER = reg("earthquake_caller", "EARTHQUAKE");
    public static final RegistryObject<Item> ACID_RAIN_CALLER = reg("acid_rain_caller", "ACID_RAIN");
    public static final RegistryObject<Item> TORNADO_CALLER = reg("tornado_caller", "TORNADO");
    public static final RegistryObject<Item> LANDSLIDE_CALLER = reg("landslide_caller", "LANDSLIDE");
    public static final RegistryObject<Item> NUKE_CALLER = reg("nuke_caller", "NUKE");
    public static final RegistryObject<Item> TSUNAMI_CALLER = reg("tsunami_caller", "TSUNAMI");
    public static final RegistryObject<Item> VOLCANO_CALLER = reg("volcano_caller", "VOLCANO");
    public static final RegistryObject<Item> SINKHOLE_CALLER = reg("sinkhole_caller", "SINKHOLE");
    public static final RegistryObject<Item> LIGHTNING_CALLER = reg("lightning_caller", "LIGHTNING_STORM");
    public static final RegistryObject<Item> BLIZZARD_CALLER = reg("blizzard_caller", "BLIZZARD");
    public static final RegistryObject<Item> ZOMBIE_CALLER = reg("zombie_caller", "ZOMBIE_APOCALYPSE");
    public static final RegistryObject<Item> DISASTER_STOPPER = reg("disaster_stopper", "STOP");

    private static RegistryObject<Item> reg(String name, String type) {
        return ITEMS.register(name, () -> new DisasterItem(new Item.Properties().setId(ITEMS.key(name)).stacksTo(1), type));
    }

    public static final RegistryObject<CreativeModeTab> DISASTER_TAB = TABS.register("disaster_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.naturaldisasters"))
        .icon(() -> new ItemStack(NUKE_CALLER.get()))
        .displayItems((p, out) -> {
            out.accept(METEOR_CALLER.get()); out.accept(EARTHQUAKE_CALLER.get()); out.accept(ACID_RAIN_CALLER.get());
            out.accept(TORNADO_CALLER.get()); out.accept(LANDSLIDE_CALLER.get()); out.accept(NUKE_CALLER.get());
            out.accept(TSUNAMI_CALLER.get()); out.accept(VOLCANO_CALLER.get()); out.accept(SINKHOLE_CALLER.get());
            out.accept(LIGHTNING_CALLER.get()); out.accept(BLIZZARD_CALLER.get()); out.accept(ZOMBIE_CALLER.get());
            out.accept(DISASTER_STOPPER.get());
        }).build()
    );

    public NaturalDisastersMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }
