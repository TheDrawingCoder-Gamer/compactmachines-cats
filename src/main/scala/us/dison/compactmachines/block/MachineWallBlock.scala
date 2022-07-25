package us.dison.compactmachines.block 

import net.minecraft.block._
import net.minecraft.block.entity.BlockEntity 
import net.minecraft.block.entity.BlockEntityTicker 
import net.minecraft.block.entity.BlockEntityType 
import net.minecraft.util.math.BlockPos 
import net.minecraft.world.World 
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.entity.MachineWallBlockEntity 
import us.dison.compactmachines.block.AbstractWallBlock
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Hand
import net.minecraft.entity.player.PlayerEntity
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity.apply
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import us.dison.compactmachines.enums.TunnelDirection
import us.dison.compactmachines.item.TunnelItem
import us.dison.compactmachines.util.TunnelUtil
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity

class MachineWallBlock(settings: AbstractBlock.Settings, breakable: Boolean) extends AbstractWallBlock(settings, breakable):
  @annotation.nowarn("cat=deprecation")
  override def onUse(state: BlockState | Null, world: World, pos: BlockPos | Null, player: PlayerEntity | Null, hand: Hand | Null, hit: BlockHitResult | Null): ActionResult | Null =
    val s = super.onUse(state, world, pos, player, hand, hit)
    if world.getRegistryKey() ne CompactMachines.CMWORLD_KEY then return ActionResult.PASS
    world.getBlockEntity(pos) match 
      case wall : MachineWallBlockEntity => 
        wall.parentID.flatMap(CompactMachines.roomManager.getRoomByNumber(_)) match 
          case None => ActionResult.FAIL 
          case Some(room) => 
            player.getStackInHand(hand).getItem() match 
              case _ : TunnelItem => 
                val stackNbt = player.getStackInHand(hand).getNbt()
                TunnelUtil.typeFromNbt(stackNbt).foreach(tunnelType => 
                  CompactMachines.LOGGER.info("The type nbt exists")
                  world.breakBlock(pos, false) 
                  world.setBlockState(pos, 
                    CompactMachines.BLOCK_WALL_TUNNEL.getDefaultState(),
                    Block.NOTIFY_ALL | Block.FORCE_STATE)
                  CompactMachines.LOGGER.info("set block state")
                  world.getBlockEntity(pos) match 
                    case tunnelE : TunnelWallBlockEntity => 
                      val tunnel = Tunnel(wall.getPos, TunnelDirection.NoDir, tunnelType, false, false, false, false)
                      CompactMachines.LOGGER.info("fetched tunnel")
                      CompactMachines.roomManager.addTunnel(room.number, tunnel)
                      CompactMachines.LOGGER.info("add tunnel")
                      // safety: we already pattern matched on it nerd
                      tunnelE.setParentID(wall.parentID.get)
                      tunnelE.setTunnel(tunnel) 
                    case _ => 
                      CompactMachines.LOGGER.warn("Tunnel wall doesn't have an entity")
                )
                ActionResult.SUCCESS
              case _ => ActionResult.PASS
                
      case _ => s 
  override def getTicker[T <: BlockEntity](world: World | Null, state: BlockState | Null, entityType: BlockEntityType[T] | Null) : BlockEntityTicker[T] | Null = 
    BlockWithEntity.checkType(entityType, CompactMachines.MACHINE_WALL_BLOCK_ENTITY, MachineWallBlockEntity.tick)
  override def createBlockEntity(pos: BlockPos | Null, state: BlockState | Null) : BlockEntity | Null = 
    MachineWallBlockEntity(pos, state)
