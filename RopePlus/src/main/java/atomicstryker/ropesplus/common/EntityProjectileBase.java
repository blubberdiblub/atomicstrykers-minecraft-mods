package atomicstryker.ropesplus.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class EntityProjectileBase extends Entity implements IProjectile
{
    protected final int TICKS_BEFORE_COLLIDABLE = 5;
    
    public float speed;
    public float slowdown;
    public float curvature;
    public float precision;
    public float hitBoxSize;
    public int configuredDamage;
    public int dmg;
    public ItemStack item;
    public int ttlInGround;
    public int xTile;
    public int yTile;
    public int zTile;
    public Block inTileBlockID;
    public int inTileMetadata;
    public boolean inGround;
    public int arrowShake;
    public EntityLivingBase shooter;
    public int ticksInGround;
    public int ticksFlying;
    public boolean shotByPlayer;

    public EntityProjectileBase(World world)
    {
        super(world);
        shooter = null;
    }

    public EntityProjectileBase(World world, EntityLivingBase EntityLivingBase, float power)
    {
        this(world);
        shooter = EntityLivingBase;
        shotByPlayer = EntityLivingBase instanceof EntityPlayer;
        setLocationAndAngles(EntityLivingBase.posX, EntityLivingBase.posY + (double) EntityLivingBase.getEyeHeight(), EntityLivingBase.posZ, EntityLivingBase.rotationYaw, EntityLivingBase.rotationPitch);
        posX -= MathHelper.cos((rotationYaw / 180F) * 3.141593F) * 0.16F;
        posY -= 0.10000000149011612D;
        posZ -= MathHelper.sin((rotationYaw / 180F) * 3.141593F) * 0.16F;
        setPosition(posX, posY, posZ);
        motionX = -MathHelper.sin((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F);
        motionZ = MathHelper.cos((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F);
        motionY = -MathHelper.sin((rotationPitch / 180F) * 3.141593F);
        setThrowableHeading(motionX, motionY, motionZ, speed*power, precision);
    }
    
    public EntityProjectileBase newArrow(World world, EntityLivingBase entityLivingBase, float power)
    {
        try
        {
            return getClass().getConstructor(new Class[] { World.class, EntityLivingBase.class, float.class }).newInstance(new Object[] { world, entityLivingBase, power });
        }
        catch (Throwable throwable)
        {
            throw new RuntimeException("Could not construct arrow instance", throwable);
        }
    }

    @Override
    protected void entityInit()
    {
        xTile = -1;
        yTile = -1;
        zTile = -1;
        inTileBlockID = Blocks.air;
        inGround = false;
        arrowShake = 0;
        ticksFlying = 0;
        setSize(0.5F, 0.5F);
        yOffset = 0.0F;
        hitBoxSize = 0.3F;
        speed = 1.0F;
        slowdown = 0.99F;
        curvature = 0.03F;
        dmg = configuredDamage;//4;
        precision = 1.0F;
        ttlInGround = 1200;
        item = null;
        
        dataWatcher.addObject(16, Byte.valueOf((byte)0));
    }

    @Override
    public void setDead()
    {
        super.setDead();
    }
    
    public void setIsCritical(boolean set)
    {
        if (!worldObj.isRemote)
        {
            byte curByte = this.dataWatcher.getWatchableObjectByte(16);
            if (set)
            {
                this.dataWatcher.updateObject(16, Byte.valueOf((byte)(curByte | 1)));
            }
            else
            {
                this.dataWatcher.updateObject(16, Byte.valueOf((byte)(curByte & -2)));
            }
        }
    }

    public boolean getIsCritical()
    {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    @Override
    public void setThrowableHeading(double motX, double motY, double motZ, float speedMod, float precisionMod)
    {
        float f2 = MathHelper.sqrt_double(motX * motX + motY * motY + motZ * motZ);
        motX /= f2;
        motY /= f2;
        motZ /= f2;
        motX += rand.nextGaussian() * 0.0075D * (double) precisionMod;
        motY += rand.nextGaussian() * 0.0075D * (double) precisionMod;
        motZ += rand.nextGaussian() * 0.0075D * (double) precisionMod;
        motX *= speedMod;
        motY *= speedMod;
        motZ *= speedMod;
        motionX = motX;
        motionY = motY;
        motionZ = motZ;
        float flatDistance = MathHelper.sqrt_double(motX * motX + motZ * motZ);
        prevRotationYaw = rotationYaw = (float) ((Math.atan2(motX, motZ) * 180D) / 3.1415927410125732D);
        prevRotationPitch = rotationPitch = (float) ((Math.atan2(motY, flatDistance) * 180D) / 3.1415927410125732D);
        ticksInGround = 0;
    }
    
    @Override
    public void setVelocity(double vX, double vY, double vZ)
    {
        this.motionX = vX;
        this.motionY = vY;
        this.motionZ = vZ;

        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
        {
            float flatDistance = MathHelper.sqrt_double(vX * vX + vZ * vZ);
            this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(vX, vZ) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(vY, (double)flatDistance) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            this.ticksInGround = 0;
        }
    }
    
    protected float calculateArrowDamage()
    {
        float damage = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        damage *= dmg;
        if (getIsCritical())
        {
            damage *= 2;
        }
        return damage;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (prevRotationPitch == 0.0F && prevRotationYaw == 0.0F)
        {
            float f = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
            prevRotationYaw = rotationYaw = (float) ((Math.atan2(motionX, motionZ) * 180D) / 3.1415927410125732D);
            prevRotationPitch = rotationPitch = (float) ((Math.atan2(motionY, f) * 180D) / 3.1415927410125732D);
        }
        if (arrowShake > 0)
        {
            arrowShake--;
        }
        
        if (inGround) // handle being stuck in the ground
        {
            Block blockID = worldObj.getBlock(xTile, yTile, zTile);
            int blockMeta = worldObj.getBlockMetadata(xTile, yTile, zTile);
            if (blockID != inTileBlockID || blockMeta != inTileMetadata)
            {
                inGround = false;
                motionX *= rand.nextFloat() * 0.2F;
                motionY *= rand.nextFloat() * 0.2F;
                motionZ *= rand.nextFloat() * 0.2F;
                ticksInGround = 0;
                ticksFlying = 0;
            }
            else
            {
                ticksInGround++;
                tickInGround();
                if (ticksInGround == ttlInGround)
                {
                    setDead();
                }
                return;
            }
        }
        else // else fly!
        {
            tickFlying();
            ticksFlying++;
        }
        
        // calculate entity hit
        Vec3 currentPosVec = worldObj.getWorldVec3Pool().getVecFromPool(posX, posY, posZ);
        Vec3 nextPosVec = worldObj.getWorldVec3Pool().getVecFromPool(posX + motionX, posY + motionY, posZ + motionZ);
        MovingObjectPosition collisionPosition = worldObj.func_147447_a(currentPosVec, nextPosVec, true, false, false);
        currentPosVec = worldObj.getWorldVec3Pool().getVecFromPool(posX, posY, posZ);
        nextPosVec = worldObj.getWorldVec3Pool().getVecFromPool(posX + motionX, posY + motionY, posZ + motionZ);
        if (collisionPosition != null)
        {
            nextPosVec = worldObj.getWorldVec3Pool().getVecFromPool(collisionPosition.hitVec.xCoord, collisionPosition.hitVec.yCoord, collisionPosition.hitVec.zCoord);
        }
        
        Entity entityHit = null;
        @SuppressWarnings("rawtypes")
        List possibleHitsList = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D));
        double nearestHit = 0.0D;
        for (int k = 0; k < possibleHitsList.size(); k++)
        {
            Entity possibleHitEnt = (Entity) possibleHitsList.get(k);
            if (canBeShot(possibleHitEnt))
            {
                float f3 = hitBoxSize;
                AxisAlignedBB axisalignedbb = possibleHitEnt.boundingBox.expand(f3, f3, f3);
                MovingObjectPosition mopCollision = axisalignedbb.calculateIntercept(currentPosVec, nextPosVec);
                
                if (mopCollision != null)
                {
                    double dist = currentPosVec.distanceTo(mopCollision.hitVec);
                    if (dist < nearestHit || nearestHit == 0.0D)
                    {
                        entityHit = possibleHitEnt;
                        nearestHit = dist;
                    }
                }
            }
        }
        
        // process entity hit
        if (entityHit != null)
        {
            if (onHitTarget(entityHit))
            {
                float damage = calculateArrowDamage();
                if (shooter != null)
                {
                    entityHit.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) shooter), damage);
                }
                else
                {
                    entityHit.attackEntityFrom((new EntityDamageSourceIndirect("arrow", this, this)).setProjectile(), damage);
                }

                setDead();
            }
        }
        else if (collisionPosition != null && collisionPosition.typeOfHit == MovingObjectType.BLOCK)
        {
            xTile = collisionPosition.blockX;
            yTile = collisionPosition.blockY;
            zTile = collisionPosition.blockZ;
            inTileBlockID = worldObj.getBlock(xTile, yTile, zTile);
            inTileMetadata = worldObj.getBlockMetadata(xTile, yTile, zTile);
            
            if (onHitBlock(xTile, yTile, zTile))
            {
                motionX = (float) (collisionPosition.hitVec.xCoord - posX);
                motionY = (float) (collisionPosition.hitVec.yCoord - posY);
                motionZ = (float) (collisionPosition.hitVec.zCoord - posZ);
                float distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
                posX -= (motionX / (double) distance) * 0.05000000074505806D;
                posY -= (motionY / (double) distance) * 0.05000000074505806D;
                posZ -= (motionZ / (double) distance) * 0.05000000074505806D;
                
                inGround = true;
                arrowShake = 7;
                setIsCritical(false);
                
                if (inTileBlockID != Blocks.air)
                {
                    inTileBlockID.onEntityCollidedWithBlock(this.worldObj, this.xTile, this.yTile, this.zTile, this);
                }
            }
            else
            {
                inTileBlockID = Blocks.air;
                inTileMetadata = 0;
            }
        }

        posX += motionX;
        posY += motionY;
        posZ += motionZ;
        float flatdistance = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) ((Math.atan2(motionX, motionZ) * 180D) / 3.1415927410125732D);
        for (rotationPitch = (float) ((Math.atan2(motionY, flatdistance) * 180D) / 3.1415927410125732D); rotationPitch - prevRotationPitch < -180F;)
        {
            prevRotationPitch -= 360F;
        }
        for (; rotationPitch - prevRotationPitch >= 180F;)
        {
            prevRotationPitch += 360F;
        }
        for (; rotationYaw - prevRotationYaw < -180F;)
        {
            prevRotationYaw -= 360F;
        }
        for (; rotationYaw - prevRotationYaw >= 180F;)
        {
            prevRotationYaw += 360F;
        }
        rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
        rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
        
        handleMotionUpdate();
        setPosition(posX, posY, posZ);
    }

    public void handleMotionUpdate()
    {
        float slow = slowdown;
        if (handleWaterMovement())
        {
            for (int i = 0; i < 4; i++)
            {
                float f1 = 0.25F;
                worldObj.spawnParticle("bubble", posX - motionX * (double) f1, posY - motionY * (double) f1, posZ - motionZ * (double) f1, motionX, motionY, motionZ);
            }

            slow *= 0.8F;
        }
        motionX *= slow;
        motionY *= slow;
        motionZ *= slow;
        motionY -= curvature;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.setShort("xTile", (short) xTile);
        nbttagcompound.setShort("yTile", (short) yTile);
        nbttagcompound.setShort("zTile", (short) zTile);
        nbttagcompound.setByte("shake", (byte) arrowShake);
        nbttagcompound.setByte("inGround", (byte) (inGround ? 1 : 0));
        nbttagcompound.setBoolean("player", shotByPlayer);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        xTile = nbttagcompound.getShort("xTile");
        yTile = nbttagcompound.getShort("yTile");
        zTile = nbttagcompound.getShort("zTile");
        arrowShake = nbttagcompound.getByte("shake") & 0xff;
        inGround = nbttagcompound.getByte("inGround") == 1;
        shotByPlayer = nbttagcompound.getBoolean("player");
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer entityplayer)
    {
        if (item == null)
        {
            return;
        }
        if (worldObj.isRemote)
        {
            return;
        }
        if (inGround && shotByPlayer && arrowShake <= 0 && entityplayer.inventory.addItemStackToInventory(item.copy()))
        {
            worldObj.playSoundAtEntity(this, "random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            entityplayer.onItemPickup(this, 1);
            setDead();
        }
    }

    public boolean canBeShot(Entity entity)
    {
        return entity.canBeCollidedWith() && ((!worldObj.isRemote && entity != shooter) || ticksFlying >= TICKS_BEFORE_COLLIDABLE);
    }
    
    /**
     * fired every tick the arrow is in midflight
     */
    public void tickFlying()
    {
        if (getIsCritical())
        {
            for (int i = 0; i < 4; ++i)
            {
                this.worldObj.spawnParticle("crit",
                        this.posX + this.motionX * (double) i / 4.0D,
                        this.posY + this.motionY * (double) i / 4.0D,
                        this.posZ + this.motionZ * (double) i / 4.0D,
                        -this.motionX, -this.motionY + 0.2D, -this.motionZ);
            }
        }
    }

    /**
     * fired every tick the arrow is stuck in a Block until it despawns (1200 ticks)
     */
    public void tickInGround()
    {
        
    }
    
    /**
     * fired when the arrow strikes an Entity
     * @param entity that was just hit by the arrow
     * @return true if the hit was accepted, false if the arrow should continue flight
     */
    public boolean onHitTarget(Entity entity)
    {
        if (entity instanceof EntityLivingBase)
        {
            ((EntityLivingBase)entity).setArrowCountInEntity(((EntityLivingBase)entity).getArrowCountInEntity() + 1);
            worldObj.playSoundAtEntity(this, "random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
            return true;
        }
        return false;
    }
    
    /**
     * fired when the arrow strikes a Block
     * @param blockX coordinate
     * @param blockY coordinate
     * @param blockZ coordinate
     * @return true if the hit was accepted, false if the arrow should continue flight
     */
    public boolean onHitBlock(int blockX, int blockY, int blockZ)
    {
        worldObj.playSoundAtEntity(this, "random.bowhit", 1.0F, 1.2F / (rand.nextFloat() * 0.2F + 0.9F));
        return true;
    }
}
