package com.example.disasters;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod(modid = NaturalDisastersMod.MODID, name = "Natural Disasters Mod", version = "1.0.0")
public class NaturalDisastersMod {
    public static final String MODID = "naturaldisasters";
    private static int tickCounter = 0, disasterDuration = 0, tsunamiWaveX = 0;
    private static String activeDisaster = "NONE";
    private static Vec3d tornadoPos = null;
    private static BlockPos targetPos = null;
    private static final Random RANDOM = new Random();
    private static final List<Item> ITEM_LIST = new ArrayList<Item>();

    public static final CreativeTabs DISASTER_TAB = new CreativeTabs("naturaldisasters") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(NUKE_CALLER);
        }
    };

    public static final Item METEOR_CALLER = reg("meteor_caller", "METEOR");
    public static final Item EARTHQUAKE_CALLER = reg("earthquake_caller", "EARTHQUAKE");
    public static final Item ACID_RAIN_CALLER = reg("acid_rain_caller", "ACID_RAIN");
    public static final Item TORNADO_CALLER = reg("tornado_caller", "TORNADO");
    public static final Item LANDSLIDE_CALLER = reg("landslide_caller", "LANDSLIDE");
    public static final Item NUKE_CALLER = reg("nuke_caller", "NUKE");
    public static final Item TSUNAMI_CALLER = reg("tsunami_caller", "TSUNAMI");
    public static final Item VOLCANO_CALLER = reg("volcano_caller", "VOLCANO");
    public static final Item SINKHOLE_CALLER = reg("sinkhole_caller", "SINKHOLE");
    public static final Item LIGHTNING_CALLER = reg("lightning_caller", "LIGHTNING_STORM");
    public static final Item BLIZZARD_CALLER = reg("blizzard_caller", "BLIZZARD");
    public static final Item ZOMBIE_CALLER = reg("zombie_caller", "ZOMBIE_APOCALYPSE");
    public static final Item DISASTER_STOPPER = reg("disaster_stopper", "STOP");

    private static Item reg(String name, String type) {
        Item item = new DisasterItem(name, type);
        ITEM_LIST.add(item);
        return item;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ITEM_LIST) {
            event.getRegistry().register(item);
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public String getName() { return "disaster"; }
            @Override
            public String getUsage(ICommandSender sender) { return "/disaster <type>"; }
            @Override
            public int getRequiredPermissionLevel() { return 2; }
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
                if (args.length > 0 && sender.getEntityWorld() instanceof WorldServer) {
                    startDisaster((WorldServer) sender.getEntityWorld(), args[0].toUpperCase(), sender.getPosition());
                }
            }
        });
    }

    public static class DisasterItem extends Item {
        private final String type;
        public DisasterItem(String name, String type) {
            this.type = type;
            this.setRegistryName(MODID, name);
            this.setUnlocalizedName(MODID + "." + name);
            this.setMaxStackSize(1);
            this.setCreativeTab(DISASTER_TAB);
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!world.isRemote && world instanceof WorldServer) {
                if (type.equals("STOP")) {
                    activeDisaster = "NONE"; disasterDuration = 0; tornadoPos = null; targetPos = null;
                    player.sendMessage(new TextComponentString("§a[HỆ THỐNG] §fĐã dừng toàn bộ thảm họa!"));
                } else {
                    startDisaster((WorldServer) world, type, player.getPosition());
                }
                player.getCooldownTracker().setCooldown(this, 40);
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
    }
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) return;
        WorldServer level = (WorldServer) event.world;
        if (level.provider.getDimension() != 0 || level.playerEntities.isEmpty()) return;

        if (++tickCounter >= 6000) {
            tickCounter = 0;
            String[] list = {"METEOR", "EARTHQUAKE", "ACID_RAIN", "TORNADO", "LANDSLIDE", "NUKE", "TSUNAMI", "VOLCANO", "SINKHOLE", "LIGHTNING_STORM", "BLIZZARD", "ZOMBIE_APOCALYPSE"};
            EntityPlayer rp = level.playerEntities.get(RANDOM.nextInt(level.playerEntities.size()));
            startDisaster(level, list[RANDOM.nextInt(list.length)], rp.getPosition());
        }

        if (disasterDuration > 0) {
            disasterDuration--;
            if ("TORNADO".equals(activeDisaster)) { if (tornadoPos != null) runTornado(level); }
            else if ("NUKE".equals(activeDisaster)) { if (targetPos != null) runNuke(level); }
            else if ("TSUNAMI".equals(activeDisaster)) { if (targetPos != null) runTsunami(level); }
            else if ("VOLCANO".equals(activeDisaster)) { if (targetPos != null) runVolcano(level); }
            else if ("SINKHOLE".equals(activeDisaster)) { if (targetPos != null) runSinkhole(level); }
            else {
                for (EntityPlayer p : level.playerEntities) runPlayerDisaster(level, p);
            }
            if (disasterDuration == 0) { activeDisaster = "NONE"; tornadoPos = null; targetPos = null; }
        }
    }

    public static void startDisaster(WorldServer level, String type, BlockPos pos) {
        activeDisaster = type;
        disasterDuration = type.equals("NUKE") ? 200 : (type.equals("SINKHOLE") ? 120 : 400);
        if (type.equals("TORNADO")) tornadoPos = new Vec3d(pos.getX() + 15, pos.getY(), pos.getZ() + 15);
        else if (type.equals("NUKE")) targetPos = pos.add(RANDOM.nextInt(16) - 8, 0, RANDOM.nextInt(16) - 8);
        else if (type.equals("TSUNAMI")) { targetPos = pos.add(-35, 0, 0); tsunamiWaveX = 0; }
        else if (type.equals("VOLCANO")) targetPos = pos.add(24, 0, 24);
        else if (type.equals("SINKHOLE")) targetPos = pos;
        else if (type.equals("ZOMBIE_APOCALYPSE")) cmd(level, "time set midnight");
        for (EntityPlayer p : level.playerEntities) p.sendMessage(new TextComponentString("§c[CẢNH BÁO] §eThiên tai: §l" + type));
    }

    private static void cmd(WorldServer l, String c) {
        if (l.getMinecraftServer() != null) {
            l.getMinecraftServer().getCommandManager().executeCommand(l.getMinecraftServer(), c);
        }
    }

    private static void runTsunami(WorldServer level) {
        if (disasterDuration % 4 == 0 && tsunamiWaveX < 70) {
            int cx = targetPos.getX() + tsunamiWaveX;
            for (int z = -25; z <= 25; z++) {
                for (int y = 0; y < 14; y++) {
                    BlockPos wp = new BlockPos(cx, targetPos.getY() + y, targetPos.getZ() + z);
                    if (level.getBlockState(wp).getMaterial().isReplaceable()) {
                        level.setBlockState(wp, Blocks.WATER.getDefaultState());
                    }
                }
            }
            tsunamiWaveX++;
        }
    }

    private static void runVolcano(WorldServer level) {
        if (disasterDuration > 240) {
            int h = (400 - disasterDuration) / 5;
            int r = Math.max(4, 26 - (int)(h * 0.7));
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= r * r) {
                        BlockPos bp = targetPos.add(x, h, z);
                        boolean isCore = (x * x + z * z <= 12 && h > 8);
                        level.setBlockState(bp, isCore ? Blocks.LAVA.getDefaultState() : (RANDOM.nextBoolean() ? Blocks.COBBLESTONE.getDefaultState() : Blocks.OBSIDIAN.getDefaultState()));
                    }
                }
            }
            if (disasterDuration == 241) {
                level.newExplosion(null, targetPos.getX(), targetPos.getY() + 32, targetPos.getZ(), 10.0f, true, true);
                cmd(level, "weather thunder 30");
            }
        } else {
            BlockPos top = targetPos.up(30);
            level.spawnParticle(EnumParticleTypes.SMOKE_LARGE, top.getX(), top.getY() + 3, top.getZ(), 40, 3.0, 4.0, 3.0, 0.08);
            level.spawnParticle(EnumParticleTypes.LAVA, top.getX(), top.getY() + 2, top.getZ(), 30, 3.0, 2.0, 3.0, 0.2);
            if (disasterDuration % 4 == 0 && !level.playerEntities.isEmpty()) {
                for (int i = 0; i < 4; i++) {
                    EntityLargeFireball fb = new EntityLargeFireball(level, level.playerEntities.get(0), (RANDOM.nextDouble() - 0.5) * 3.2, 1.1, (RANDOM.nextDouble() - 0.5) * 3.2);
                    fb.explosionPower = 4;
                    fb.setPosition(top.getX() + RANDOM.nextInt(5) - 2, top.getY() + 3, top.getZ() + RANDOM.nextInt(5) - 2);
                    level.spawnEntity(fb);
                }
                level.setBlockState(top.add(RANDOM.nextInt(7) - 3, 1, RANDOM.nextInt(7) - 3), Blocks.LAVA.getDefaultState());
            }
            for (int i = 0; i < 6; i++) {
                BlockPos ground = targetPos.add(RANDOM.nextInt(50) - 25, RANDOM.nextInt(6) - 2, RANDOM.nextInt(50) - 25);
                if (level.getBlockState(ground).isFullBlock() && level.isAirBlock(ground.up())) {
                    level.setBlockState(ground, Blocks.MAGMA.getDefaultState());
                    if (RANDOM.nextInt(3) == 0) level.setBlockState(ground.up(), Blocks.FIRE.getDefaultState());
                }
            }
        }
    }
    private static void runSinkhole(WorldServer level) {
        if (disasterDuration % 3 == 0) {
            int depth = (120 - disasterDuration) / 3;
            int r = 9;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= r * r) {
                        BlockPos bp = targetPos.add(x, -depth, z);
                        if (level.getBlockState(bp).getBlock() != Blocks.BEDROCK) level.setBlockToAir(bp);
                    }
                }
            }
        }
    }

    private static void runTornado(WorldServer level) {
        if (level.playerEntities.isEmpty()) return;
        Vec3d dir = level.playerEntities.get(0).getPositionVector().subtract(tornadoPos).normalize();
        tornadoPos = tornadoPos.addVector(dir.x * 0.25, 0, dir.z * 0.25);
        for (int y = 0; y < 30; y++) {
            double r = 1.5 + y * 0.25;
            for (int i = 0; i < 6; i++) {
                double a = disasterDuration * 0.4 + i * Math.PI / 3 + y * 0.2;
                level.spawnParticle(EnumParticleTypes.SMOKE_LARGE, tornadoPos.x + Math.cos(a) * r, tornadoPos.y + y, tornadoPos.z + Math.sin(a) * r, 1, 0, 0, 0, 0.01);
            }
        }
        AxisAlignedBB box = new AxisAlignedBB(tornadoPos.x - 18, tornadoPos.y - 5, tornadoPos.z - 18, tornadoPos.x + 18, tornadoPos.y + 35, tornadoPos.z + 18);
        for (Entity e : level.getEntitiesWithinAABB(Entity.class, box)) {
            double dx = tornadoPos.x - e.posX, dz = tornadoPos.z - e.posZ;
            e.motionX = dx * 0.08 - dz * 0.12;
            e.motionY = 0.35;
            e.motionZ = dz * 0.08 + dx * 0.12;
            e.velocityChanged = true;
        }
    }

    private static void runNuke(WorldServer level) {
        if (disasterDuration > 100) {
            if (disasterDuration % 20 == 0) {
                for (EntityPlayer p : level.playerEntities) {
                    p.sendMessage(new TextComponentString("§4§l[HẠT NHÂN] §eNổ sau: §c" + ((disasterDuration - 100) / 20) + "s"));
                    level.playSound(null, p.getPosition(), SoundEvents.BLOCK_NOTE_BELL, SoundCategory.MASTER, 1f, 0.5f);
                }
            }
            level.spawnParticle(EnumParticleTypes.FLAME, targetPos.getX(), targetPos.getY() + 2, targetPos.getZ(), 15, 0.5, 10, 0.5, 0.05);
        } else if (disasterDuration == 100) {
            level.newExplosion(null, targetPos.getX(), targetPos.getY(), targetPos.getZ(), 22f, true, true);
            for (EntityLivingBase le : level.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(targetPos).grow(45))) {
                le.addPotionEffect(new PotionEffect(MobEffects.WITHER, 300, 1));
                le.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 300, 0));
                le.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
            }
        } else {
            level.spawnParticle(EnumParticleTypes.SMOKE_LARGE, targetPos.getX(), targetPos.getY() + 15, targetPos.getZ(), 80, 20, 10, 20, 0.02);
        }
    }

    private static void runPlayerDisaster(WorldServer level, EntityPlayer p) {
        BlockPos pos = p.getPosition();
        if ("METEOR".equals(activeDisaster)) {
            if (disasterDuration % 20 == 0) {
                EntityLargeFireball fb = new EntityLargeFireball(level, p, 0, -1.5, 0);
                fb.explosionPower = 3;
                fb.setPosition(pos.getX() + RANDOM.nextInt(30) - 15, pos.getY() + 35, pos.getZ() + RANDOM.nextInt(30) - 15);
                level.spawnEntity(fb);
            }
        } else if ("EARTHQUAKE".equals(activeDisaster)) {
            p.motionX += (RANDOM.nextDouble() - 0.5) * 0.4;
            p.motionY += 0.15;
            p.motionZ += (RANDOM.nextDouble() - 0.5) * 0.4;
            p.velocityChanged = true;
            if (disasterDuration % 5 == 0) {
                BlockPos cp = pos.add(RANDOM.nextInt(12) - 6, -1, RANDOM.nextInt(12) - 6);
                if (!level.isAirBlock(cp) && level.getBlockState(cp).getBlockHardness(level, cp) >= 0) level.destroyBlock(cp, false);
            }
        } else if ("ACID_RAIN".equals(activeDisaster)) {
            cmd(level, "weather thunder 20");
            if (level.canSeeSky(pos)) {
                p.addPotionEffect(new PotionEffect(MobEffects.POISON, 40, 1));
                if (level.getBlockState(pos.down()).getBlock() == Blocks.GRASS) level.setBlockState(pos.down(), Blocks.DIRT.getDefaultState());
            }
        } else if ("LANDSLIDE".equals(activeDisaster)) {
            if (disasterDuration % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    BlockPos sp = pos.add(RANDOM.nextInt(20) - 10, RANDOM.nextInt(8) + 2, RANDOM.nextInt(20) - 10);
                    if (!level.isAirBlock(sp)) {
                        EntityFallingBlock fb = new EntityFallingBlock(level, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, level.getBlockState(sp));
                        level.setBlockToAir(sp);
                        level.spawnEntity(fb);
                    }
                }
            }
        } else if ("LIGHTNING_STORM".equals(activeDisaster)) {
            cmd(level, "weather thunder 20");
            if (disasterDuration % 10 == 0) {
                BlockPos lp = pos.add(RANDOM.nextInt(32) - 16, 0, RANDOM.nextInt(32) - 16);
                cmd(level, "summon minecraft:lightning_bolt " + lp.getX() + " " + lp.getY() + " " + lp.getZ());
            }
        } else if ("BLIZZARD".equals(activeDisaster)) {
            level.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, pos.getX(), pos.getY() + 6, pos.getZ(), 60, 12, 5, 12, 0.05);
            p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1));
            if (disasterDuration % 10 == 0 && level.canSeeSky(pos)) p.attackEntityFrom(DamageSource.GENERIC, 1.0f);
            for (int i = 0; i < 5; i++) {
                BlockPos fp = pos.add(RANDOM.nextInt(20) - 10, -1, RANDOM.nextInt(20) - 10);
                if (level.getBlockState(fp).getBlock() == Blocks.WATER) level.setBlockState(fp, Blocks.ICE.getDefaultState());
                else if (level.isAirBlock(fp.up()) && level.getBlockState(fp).isFullBlock()) level.setBlockState(fp.up(), Blocks.SNOW_LAYER.getDefaultState());
            }
        } else if ("ZOMBIE_APOCALYPSE".equals(activeDisaster)) {
            if (disasterDuration % 40 == 0) {
                BlockPos zp = pos.add(RANDOM.nextInt(24) - 12, 1, RANDOM.nextInt(24) - 12);
                cmd(level, "summon minecraft:zombie " + zp.getX() + " " + zp.getY() + " " + zp.getZ());
            }
        }
    }
}
