package com.example.disasters;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import java.util.*;

@Mod("naturaldisasters")
public class NaturalDisastersMod {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "naturaldisasters");
    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    public static final ItemGroup TAB = new ItemGroup("naturaldisasters") {
        @Override public ItemStack makeIcon() { return new ItemStack(Items.FIRE_CHARGE); }
    };

    private static RegistryObject<Item> reg(String id, String t) {
        RegistryObject<Item> o = ITEMS.register(id, () -> new DisasterItem(new Item.Properties().tab(TAB).stacksTo(1), t));
        ALL.add(o);
        return o;
    }

    public static final RegistryObject<Item> I1 = reg("tornado_spawner", "tornado"), I2 = reg("meteor_spawner", "meteor"),
            I3 = reg("earthquake_spawner", "earthquake"), I4 = reg("volcano_spawner", "volcano"),
            I5 = reg("lightning_spawner", "lightning"), I6 = reg("tsunami_spawner", "tsunami"),
            I7 = reg("blizzard_spawner", "blizzard"), I8 = reg("blackhole_spawner", "blackhole"),
            I9 = reg("acidrain_spawner", "acidrain"), I10 = reg("nuke_spawner", "nuke"),
            I11 = reg("sandstorm_spawner", "sandstorm"), I12 = reg("wildfire_spawner", "wildfire");

    private static Vector3d tPos = null, bPos = null;
    private static int tTick = 0, bTick = 0;

    public NaturalDisastersMod() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static class DisasterItem extends Item {
        private final String type;
        public DisasterItem(Properties p, String t) { super(p); this.type = t; }
        @Override public ActionResult<ItemStack> use(World w, PlayerEntity p, Hand h) {
            if (!w.isClientSide && w instanceof ServerWorld) run((ServerWorld) w, p, p.blockPosition(), type);
            return ActionResult.sidedSuccess(p.getItemInHand(h), w.isClientSide);
        }
    }
    private static void run(ServerWorld w, PlayerEntity p, BlockPos pos, String type) {
        Random r = w.random;
        switch (type) {
            case "tornado": tPos = new Vector3d(pos.getX() + 8, pos.getY(), pos.getZ() + 8); tTick = 500; break;
            case "meteor":
                for (int i = 0; i != 10; i++) {
                    FireballEntity m = new FireballEntity(w, pos.getX() + (r.nextDouble() - 0.5) * 35, pos.getY() + 60 + i * 5, pos.getZ() + (r.nextDouble() - 0.5) * 35, 0, -1.5, 0);
                    m.explosionPower = 7; w.addFreshEntity(m);
                } break;
            case "earthquake":
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0)
                    for (int y = -6; y != 4; y++) { BlockPos t = pos.offset(x, y, z); if (!w.isEmptyBlock(t) && w.getBlockState(t).getBlock() != Blocks.BEDROCK) w.destroyBlock(t, false); }
                w.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 0.5F); break;
            case "volcano":
                int rad = 14;
                for (int y = 0; y != rad + 1; y++) { int lr = rad - y;
                    for (int x = -lr; x != lr + 1; x++) for (int z = -lr; z != lr + 1; z++) {
                        int d = x * x + z * z, m = lr * lr, in = (lr - 2) * (lr - 2);
                        if (d == m || m - d > 0) w.setBlockAndUpdate(pos.offset(x + 16, y, z), (lr > 2 && y > 2 && in - d > 0 ? Blocks.LAVA : Blocks.OBSIDIAN).defaultBlockState());
                    }
                }
                for (int i = 0; i != 15; i++) {
                    TNTEntity t = new TNTEntity(w, pos.getX() + 16, pos.getY() + rad + 3, pos.getZ(), p);
                    t.setDeltaMovement((r.nextDouble() - 0.5) * 1.8, 1.1 + r.nextDouble(), (r.nextDouble() - 0.5) * 1.8); t.setFuse(60); w.addFreshEntity(t);
                } break;
            case "lightning":
                for (int i = 0; i != 20; i++) {
                    BlockPos top = w.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, new BlockPos(pos.getX() + (int) ((r.nextDouble() - 0.5) * 45), 0, pos.getZ() + (int) ((r.nextDouble() - 0.5) * 45)));
                    LightningBoltEntity b = EntityType.LIGHTNING_BOLT.create(w);
                    if (b != null) { b.moveTo(Vector3d.atBottomCenterOf(top)); w.addFreshEntity(b); }
                } break;
            case "tsunami":
                for (int x = -20; x != 21; x++) for (int z = -20; z != 21; z++) for (int y = 0; y != 10; y++) { BlockPos t = pos.offset(x, y, z); if (w.isEmptyBlock(t)) w.setBlockAndUpdate(t, Blocks.WATER.defaultBlockState()); } break;
            case "blizzard":
                for (int x = -20; x != 21; x++) for (int z = -20; z != 21; z++) {
                    BlockPos top = w.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pos.offset(x, 0, z));
                    if (w.isEmptyBlock(top)) w.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                    if (w.getBlockState(top.below()).getBlock() == Blocks.WATER) w.setBlockAndUpdate(top.below(), Blocks.ICE.defaultBlockState());
                }
                for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(pos).inflate(20))) e.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 300, 3)); break;
            case "blackhole": bPos = new Vector3d(pos.getX() + 10, pos.getY() + 8, pos.getZ() + 10); bTick = 300; break;
            case "acidrain":
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0) {
                    BlockPos top = w.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pos.offset(x, 0, z)).below();
                    if (w.getBlockState(top).getBlock() != Blocks.BEDROCK) w.removeBlock(top, false);
                }
                for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(pos).inflate(20))) e.addEffect(new EffectInstance(Effects.POISON, 200, 2)); break;
            case "nuke": w.explode(p, pos.getX() + 12, pos.getY(), pos.getZ() + 12, 22.0F, true, Explosion.Mode.DESTROY); break;
            case "sandstorm":
                for (int i = 0; i != 80; i++) w.addFreshEntity(new FallingBlockEntity(w, pos.getX() + (r.nextDouble() - 0.5) * 30, pos.getY() + 15 + r.nextInt(10), pos.getZ() + (r.nextDouble() - 0.5) * 30, Blocks.SAND.defaultBlockState()));
                for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(pos).inflate(20))) e.addEffect(new EffectInstance(Effects.BLINDNESS, 200, 1)); break;
            case "wildfire":
                for (int x = -18; x != 19; x++) for (int z = -18; z != 19; z++) if (r.nextInt(3) == 0) {
                    BlockPos top = w.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pos.offset(x, 0, z));
                    if (w.isEmptyBlock(top)) w.setBlockAndUpdate(top, Blocks.FIRE.defaultBlockState());
                } break;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.world instanceof ServerWorld)) return;
        ServerWorld w = (ServerWorld) e.world; Random r = w.random;
        if (tTick > 0 && tPos != null) {
            tTick--; Vector3d d = new Vector3d(r.nextDouble() - 0.5, 0, r.nextDouble() - 0.5).normalize();
            tPos = tPos.add(d.x * 0.35, 0, d.z * 0.35); BlockPos c = new BlockPos(tPos);
            for (int y = 0; y != 22; y += 2) { double rad = 1.5 + y * 0.35, ang = tTick * 0.6 + y * 0.5; w.sendParticles(ParticleTypes.CLOUD, tPos.x + Math.cos(ang) * rad, tPos.y + y, tPos.z + Math.sin(ang) * rad, 4, 0.4, 0.2, 0.4, 0.02); }
            if (tTick % 15 == 0) w.playSound(null, c, SoundEvents.ENDER_DRAGON_FLAP, SoundCategory.WEATHER, 3.0F, 0.5F);
            for (int y = 0; y != 20; y++) { int rad = 2 + y / 4; for (int x = -rad; x != rad + 1; x++) for (int z = -rad; z != rad + 1; z++) if (r.nextInt(10) == 0) { BlockPos bp = c.offset(x, y, z); if (!w.isEmptyBlock(bp) && w.getBlockState(bp).getBlock() != Blocks.BEDROCK) w.removeBlock(bp, false); } }
            for (Entity en : w.getEntities(null, new AxisAlignedBB(c).inflate(10, 24, 10))) { en.setDeltaMovement(en.getDeltaMovement().x + (r.nextDouble() - 0.5) * 1.1, 0.75, en.getDeltaMovement().z + (r.nextDouble() - 0.5) * 1.1); en.hurtMarked = true; }
        }
        if (bTick > 0 && bPos != null) {
            bTick--; BlockPos c = new BlockPos(bPos);
            w.sendParticles(ParticleTypes.PORTAL, bPos.x, bPos.y, bPos.z, 40, 2.5, 2.5, 2.5, 0.5);
            for (int i = 0; i != 12; i++) { BlockPos bp = c.offset(r.nextInt(17) - 8, r.nextInt(17) - 8, r.nextInt(17) - 8); if (!w.isEmptyBlock(bp) && w.getBlockState(bp).getBlock() != Blocks.BEDROCK) w.removeBlock(bp, false); }
            for (Entity en : w.getEntities(null, new AxisAlignedBB(c).inflate(18))) { en.setDeltaMovement(bPos.subtract(en.position()).normalize().scale(0.5)); en.hurtMarked = true; }
        }
    }
}
