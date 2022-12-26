package com.infamous.call_of_the_wild.common.sensor;

import com.google.common.collect.ImmutableSet;
import com.infamous.call_of_the_wild.common.COTWTags;
import com.infamous.call_of_the_wild.common.entity.dog.WolfAi;
import com.infamous.call_of_the_wild.common.entity.dog.WolflikeAi;
import com.infamous.call_of_the_wild.common.registry.COTWMemoryModuleTypes;
import com.infamous.call_of_the_wild.common.util.AiUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("NullableProblems")
public class WolfSpecificSensor extends Sensor<Wolf> {

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                COTWMemoryModuleTypes.NEAREST_VISIBLE_DISLIKED.get(),
                COTWMemoryModuleTypes.NEAREST_VISIBLE_HUNTABLE.get(),
                MemoryModuleType.NEAREST_ATTACKABLE,
                MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    @Override
    protected void doTick(ServerLevel level, Wolf wolf) {
        Brain<?> brain = wolf.getBrain();

        Optional<LivingEntity> nearestDisliked = Optional.empty();
        Optional<LivingEntity> nearestHuntable = Optional.empty();
        Optional<LivingEntity> nearestAttackable = Optional.empty();
        Optional<Player> nearestPlayerHoldingWantedItem = Optional.empty();

        NearestVisibleLivingEntities nvle = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());

        for (LivingEntity livingEntity : nvle.findAll((le) -> true)) {
            if(nearestDisliked.isEmpty() && WolfAi.isDisliked(wolf, livingEntity)){
                nearestDisliked = Optional.of(livingEntity);
            } else if(nearestHuntable.isEmpty()
                    && WolflikeAi.isHuntable(wolf, livingEntity, COTWTags.WOLF_HUNT_TARGETS)){
                nearestHuntable = Optional.of(livingEntity);
            } else if(nearestAttackable.isEmpty()
                    && AiUtil.isAttackable(wolf, livingEntity, COTWTags.WOLF_ALWAYS_HOSTILES)){
                nearestAttackable = Optional.of(livingEntity);
            } else if (livingEntity instanceof Player player) {
                if (nearestPlayerHoldingWantedItem.isEmpty()
                        && !WolfAi.wantsToAvoidPlayer(wolf, player)
                        && player.isHolding(is -> WolfAi.isInteresting(wolf, is))) {
                    nearestPlayerHoldingWantedItem = Optional.of(player);
                }
            }
        }

        brain.setMemory(COTWMemoryModuleTypes.NEAREST_VISIBLE_DISLIKED.get(), nearestDisliked);
        brain.setMemory(COTWMemoryModuleTypes.NEAREST_VISIBLE_HUNTABLE.get(), nearestHuntable);
        brain.setMemory(MemoryModuleType.NEAREST_ATTACKABLE, nearestAttackable);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, nearestPlayerHoldingWantedItem);
    }

}
