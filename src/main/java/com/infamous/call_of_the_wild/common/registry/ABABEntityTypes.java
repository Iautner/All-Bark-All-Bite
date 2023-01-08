package com.infamous.call_of_the_wild.common.registry;

import com.infamous.call_of_the_wild.AllBarkAllBite;
import com.infamous.call_of_the_wild.common.entity.dog.Dog;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ABABEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AllBarkAllBite.MODID);

    public static final String DOG_NAME = "dog";
    public static final RegistryObject<EntityType<Dog>> DOG = ENTITY_TYPES.register(
            DOG_NAME,
            () -> EntityType.Builder.of(Dog::new, MobCategory.CREATURE).sized(0.6F, 0.85F).clientTrackingRange(10).build(DOG_NAME)
    );
}