package ml.luxinfine.advfarmstation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Mod(modid = "spawnerspicker", acceptableRemoteVersions = "*")
public class SpawnersPickerMod {
    private static final Map<Position, String> brokenSpawners = new HashMap<>();
    private static Item brokenEIOSpawner;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        brokenEIOSpawner = GameRegistry.findItem("EnderIO", "itemBrokenSpawner");
    }

    @SubscribeEvent
    public void onEndTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            brokenSpawners.clear();
    }

    @SubscribeEvent
    public void onSpawnerBreak(BlockEvent.BreakEvent event) {
        if (event.block == Blocks.mob_spawner && event.getPlayer() != null && EnchantmentHelper.getSilkTouchModifier(event.getPlayer())) {
            TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
            if (te instanceof TileEntityMobSpawner) {
                String name = ((TileEntityMobSpawner) te).func_145881_a().getEntityNameToSpawn();
                brokenSpawners.put(new Position(event.world.provider.dimensionId, event.x, event.y, event.z), name);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW) //После EnderIO, чтобы не было дюпа поломанных спавнеров
    public void onSpawnerDrop(BlockEvent.HarvestDropsEvent event) {
        if (event.block == Blocks.mob_spawner) {
            String name = brokenSpawners.get(new Position(event.world.provider.dimensionId, event.x, event.y, event.z));
            if (name != null) {
                if (brokenEIOSpawner != null) {
                    //Удаляем сломанный спавнер, если дропаем целый
                    Iterator<ItemStack> it = event.drops.iterator();
                    while (it.hasNext()) {
                        ItemStack i = it.next();
                        if (i.getItem() == brokenEIOSpawner) {
                            it.remove();
                            break;
                        }
                    }
                }
                ItemStack spawner = new ItemStack(Blocks.mob_spawner, 1, entityName2ID(event.world, name));
                spawner.setTagInfo("EntityID", new NBTTagString(name));
                event.drops.add(spawner);
            }
        }
    }

    @SubscribeEvent
    public void onSpawnerPlace(BlockEvent.PlaceEvent event) {
        if (event.itemInHand == null || event.itemInHand.getItem() != Item.getItemFromBlock(Blocks.mob_spawner) || event.itemInHand.stackTagCompound == null || !event.itemInHand.stackTagCompound.hasKey("EntityID")) return;
        TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
        if (te instanceof TileEntityMobSpawner)
            ((TileEntityMobSpawner) te).func_145881_a().setEntityName(event.itemInHand.stackTagCompound.getString("EntityID"));
    }

    private static int entityName2ID(World world, String name) {
        try {
            return EntityList.getEntityID(EntityList.createEntityByName(name, world));
        } catch (Throwable t) {
            return 0;
        }
    }

    private static class Position {
        private final int dim, x, y, z;

        private Position(int dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false; //Unsafe
            Position position = (Position) o;
            return dim == position.dim && x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dim, x, y, z);
        }
    }
}
