package com.example.disasters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.ArrayList;
import java.util.List;

@Mod("naturaldisasters")
public class NaturalDisastersMod {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "naturaldisasters");
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "naturaldisasters");
    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private static RegistryObject<Item> reg(String id, String t) {
        RegistryObject<Item> o = ITEMS.register(id, () -> new DisasterItem(new Item.Properties().setId(ITEMS.key(id)).stacksTo(1), t));
        ALL.add(o);
        return o;
    }

    public static final RegistryObject<Item> I1 = reg("tornado_spawner", "tornado");
    public static final RegistryObject<Item> I2 = reg("meteor_spawner", "meteor");
    public static final RegistryObject<Item> I3 = reg("earthquake_spawner", "earthquake");
    public static final RegistryObject<Item> I4 = reg("volcano_spawner", "volcano");
    public static final RegistryObject<Item> I5 = reg("lightning_spawner", "lightning");
    public static final RegistryObject<Item> I6 = reg("tsunami_spawner", "tsunami");
    public static final RegistryObject<Item> I7 = reg("blizzard_spawner", "blizzard");
    public static final RegistryObject<Item> I8 = reg("blackhole_spawner", "blackhole");
    public static final RegistryObject<Item> I9 = reg("acidrain_spawner", "acidrain");
    public static final RegistryObject<Item> I10 = reg("nuke_spawner", "nuke");
    public static final RegistryObject<Item> I11 = reg("sandstorm_spawner", "sandstorm");
    public static final RegistryObject<Item> I12 = reg("wildfire_spawner", "wildfire");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("naturaldisasters", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.naturaldisasters"))
            .icon(() -> new ItemStack(Items.FIRE_CHARGE))
            .displayItems((p, out) -> ALL.forEach(i -> out.accept(i.get())))
            .build());

    private static Vec3 tPos = null, bPos = null;
    private static int tTick = 0, bTick = 0;

    public NaturalDisastersMod(FMLJavaModLoadingContext ctx) {
        var bus = ctx.getModBusGroup();
        ITEMS.register(bus);
        TABS.register(bus);
        TickEvent.LevelTickEvent.Post.BUS.addListener(this::onTick);
    }

    public static class DisasterItem extends Item {
        private final String type;
        public DisasterItem(Properties p, String t) { super(p); this.type = t; }

        @Override
        public InteractionResult use(Level l, Player p, InteractionHand h) {
            if (!l.isClientSide() && l instanceof ServerLevel sl) run(sl, p, p.blockPosition(), type);
            return InteractionResult.SUCCESS;
        }
    }
    private static void run(ServerLevel l, Player p, BlockPos pos, String type) {
        RandomSource r = l.getRandom();
        switch (type) {
            case "tornado" -> { tPos = new Vec3(pos.getX() + 8, pos.getY(), pos.getZ() + 8); tTick = 500; }
            case "meteor" -> {
                for (int i = 0; i != 10; i++) {
                    var m = new LargeFireball(l, p, new Vec3(0, -1.5, 0), 7);
                    m.setPos(pos.getX() + (r.nextDouble() - 0.5) * 35, pos.getY() + 60 + i * 5, pos.getZ() + (r.nextDouble() - 0.5) * 35);
                    l.addFreshEntity(m);
                }
            }
            case "earthquake" -> {
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0)
                    for (int y = -6; y != 4; y++) {
                        BlockPos t = pos.offset(x, y, z);
                        if (!l.isEmptyBlock(t) && !l.getBlockState(t).is(Blocks.BEDROCK)) l.destroyBlock(t, false);
                    }
                l.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.5F);
            }
            case "volcano" -> {
                int rad = 14;
                for (int y = 0; y != rad + 1; y++) {
                    int lr = rad - y;
                    for (int x = -lr; x != lr + 1; x++) for (int z = -lr; z != lr + 1; z++) {
                        int d = x * x + z * z, m = lr * lr, in = (lr - 2) * (lr - 2);
                        if (d == m || m - d > 0) l.setBlockAndUpdate(pos.offset(x + 16, y, z), (lr > 2 && y > 2 && in - d > 0 ? Blocks.LAVA : Blocks.OBSIDIAN).defaultBlockState());
                    }
                }
                for (int i = 0; i != 15; i++) {
                    var t = new PrimedTnt(l, pos.getX() + 16, pos.getY() + rad + 3, pos.getZ(), p);
                    t.setDeltaMovement((r.nextDouble() - 0.5) * 1.8, 1.1 + r.nextDouble(), (r.nextDouble() - 0.5) * 1.8);
                    t.setFuse(60);
                    l.addFreshEntity(t);
                }
            }
            case "lightning" -> {
                for (int i = 0; i != 20; i++) {
                    BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(pos.getX() + (int) ((r.nextDouble() - 0.5) * 45), 0, pos.getZ() + (int) ((r.nextDouble() - 0.5) * 45)));
                    l.getServer().getCommands().performPrefixedCommand(l.getServer().createCommandSourceStack().withSuppressedOutput(), "summon minecraft:lightning_bolt " + top.getX() + " " + top.getY() + " " + top.getZ());
                }
            }
            case "tsunami" -> {
                for (int x = -20; x != 21; x++) for (int z = -20; z != 21; z++) for (int y = 0; y != 10; y++) {
                    BlockPos t = pos.offset(x, y, z);
                    if (l.isEmptyBlock(t)) l.setBlockAndUpdate(t, Blocks.WATER.defaultBlockState());
                }
            }
            case "blizzard" -> {
                for (int x = -20; x != 21; x++) for (int z = -20; z != 21; z++) {
                    BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.offset(x, 0, z));
                    if (l.isEmptyBlock(top)) l.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                    if (l.getBlockState(top.below()).is(Blocks.WATER)) l.setBlockAndUpdate(top.below(), Blocks.ICE.defaultBlockState());
                }
                l.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(20)).forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 3)));
            }
            case "blackhole" -> { bPos = new Vec3(pos.getX() + 10, pos.getY() + 8, pos.getZ() + 10); bTick = 300; }
            case "acidrain" -> {
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0) {
                    BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.offset(x, 0, z)).below();
                    if (!l.getBlockState(top).is(Blocks.BEDROCK)) l.removeBlock(top, false);
                }
                l.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(20)).forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2)));
            }
            case "nuke" -> l.explode(p, pos.getX() + 12, pos.getY(), pos.getZ() + 12, 22.0F, true, Level.ExplosionInteraction.TNT);
            case "sandstorm" -> {
                for (int i = 0; i != 80; i++) FallingBlockEntity.fall(l, pos.offset((int) ((r.nextDouble() - 0.5) * 30), 15 + r.nextInt(10), (int) ((r.nextDouble() - 0.5) * 30)), Blocks.SAND.defaultBlockState());
                l.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(20)).forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1)));
            }
            case "wildfire" -> {
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0) {
                    BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos.offset(x, 0, z));
                    if (l.isEmptyBlock(top)) l.setBlockAndUpdate(top, Blocks.FIRE.defaultBlockState());
                }
            }
        }
    }

    public void onTick(TickEvent.LevelTickEvent.Post e) {
        if (!(e.level() instanceof ServerLevel l)) return;
        RandomSource r = l.getRandom();
        if (tTick > 0 && tPos != null) {
            tTick--;
            Vec3 d = new Vec3(r.nextDouble() - 0.5, 0, r.nextDouble() - 0.5).normalize();
            tPos = tPos.add(d.x * 0.35, 0, d.z * 0.35);
            BlockPos c = BlockPos.containing(tPos);
            for (int y = 0; y != 22; y += 2) {
                double rad = 1.5 + y * 0.35, ang = tTick * 0.6 + y * 0.5;
                l.sendParticles(ParticleTypes.CLOUD, tPos.x + Math.cos(ang) * rad, tPos.y + y, tPos.z + Math.sin(ang) * rad, 4, 0.4, 0.2, 0.4, 0.02);
            }
            if (tTick % 15 == 0) l.playSound(null, c, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.WEATHER, 3.0F, 0.5F);
            for (int y = 0; y != 20; y++) {
                int rad = 2 + y / 4;
                for (int x = -rad; x != rad + 1; x++) for (int z = -rad; z != rad + 1; z++) if (r.nextInt(10) == 0) {
                    BlockPos bp = c.offset(x, y, z);
                    if (!l.isEmptyBlock(bp) && !l.getBlockState(bp).is(Blocks.BEDROCK)) l.removeBlock(bp, false);
                }
            }
            for (Entity en : l.getEntities(null, new AABB(c).inflate(10, 24, 10))) {
                en.setDeltaMovement(en.getDeltaMovement().x + (r.nextDouble() - 0.5) * 1.1, 0.75, en.getDeltaMovement().z + (r.nextDouble() - 0.5) * 1.1);
                en.hurtMarked = true;
            }
        }
        if (bTick > 0 && bPos != null) {
            bTick--;
            BlockPos c = BlockPos.containing(bPos);
            l.sendParticles(ParticleTypes.PORTAL, bPos.x, bPos.y, bPos.z, 40, 2.5, 2.5, 2.5, 0.5);
            for (int i = 0; i != 12; i++) {
                BlockPos bp = c.offset(r.nextInt(17) - 8, r.nextInt(17) - 8, r.nextInt(17) - 8);
                if (!l.isEmptyBlock(bp) && !l.getBlockState(bp).is(Blocks.BEDROCK)) l.removeBlock(bp, false);
            }
            for (Entity en : l.getEntities(null, new AABB(c).inflate(18))) {
                en.setDeltaMovement(bPos.subtract(en.position()).normalize().scale(0.5));
                en.hurtMarked = true;
            }
        }
    }
}
