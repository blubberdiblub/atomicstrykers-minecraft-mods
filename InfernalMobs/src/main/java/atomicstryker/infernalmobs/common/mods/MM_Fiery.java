package atomicstryker.infernalmobs.common.mods;

import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

public class MM_Fiery extends MobModifier
{
    
    public MM_Fiery()
    {
        super();
    }
    
    public MM_Fiery(MobModifier next)
    {
        super(next);
    }

    @Override
    public String getModName()
    {
        return "Fiery";
    }
    
    @Override
    public float onHurt(EntityLivingBase mob, DamageSource source, float damage)
    {
        if (source.getEntity() != null
        && (source.getEntity() instanceof EntityLivingBase)
        && !(source instanceof EntityDamageSourceIndirect))
        {
            source.getEntity().setFire(3);
        }
        
        mob.extinguish();
        return super.onHurt(mob, source, damage);
    }
    
    @Override
    public float onAttack(EntityLivingBase entity, DamageSource source, float damage)
    {
        if (entity != null)
        {
            entity.setFire(3);
        }
        
        return super.onAttack(entity, source, damage);
    }
    
    @Override
    protected String[] getModNameSuffix()
    {
        return suffix;
    }
    private static String[] suffix = { "ofConflagration", "thePhoenix", "ofCrispyness" };
    
    @Override
    protected String[] getModNamePrefix()
    {
        return prefix;
    }
    private static String[] prefix = { "burning", "toasting" };
    
}
