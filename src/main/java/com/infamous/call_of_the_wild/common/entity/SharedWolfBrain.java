package com.infamous.call_of_the_wild.common.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.infamous.call_of_the_wild.common.ai.*;
import com.infamous.call_of_the_wild.common.behavior.*;
import com.infamous.call_of_the_wild.common.behavior.hunter.*;
import com.infamous.call_of_the_wild.common.behavior.pet.OwnerHurtByTarget;
import com.infamous.call_of_the_wild.common.behavior.pet.OwnerHurtTarget;
import com.infamous.call_of_the_wild.common.behavior.pet.SitWhenOrderedTo;
import com.infamous.call_of_the_wild.common.behavior.sleep.SleepOnGround;
import com.infamous.call_of_the_wild.common.registry.ABABMemoryModuleTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SharedWolfBrain {
    public static AnimalMakeLove createBreedBehavior(EntityType<? extends TamableAnimal> type) {
        return new AnimalMakeLove(type, SharedWolfAi.SPEED_MODIFIER_BREEDING);
    }

    public static <E extends TamableAnimal> ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> getFightPackage(BiPredicate<E, LivingEntity> huntTargetPredicate) {
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new Sprint<>(SharedWolfAi::canMove),
                        new LeapAtTarget(SharedWolfAi.LEAP_CHANCE, SharedWolfAi.LEAP_YD, SharedWolfAi.TOO_CLOSE_TO_LEAP, SharedWolfAi.TOO_FAR_TO_LEAP),
                        new RunIf<>(Entity::isOnGround, new SetWalkTargetFromAttackTargetIfTargetOutOfReach(SharedWolfAi.SPEED_MODIFIER_CHASING), true),
                        new MeleeAttack(SharedWolfAi.ATTACK_COOLDOWN_TICKS),
                        new RememberIfHuntTargetWasKilled<>(huntTargetPredicate, SharedWolfAi.TIME_BETWEEN_HUNTS),
                        new EraseMemoryIf<>(BehaviorUtils::isBreeding, MemoryModuleType.ATTACK_TARGET)));
    }

    public static <E extends TamableAnimal> ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> getTargetPackage(BiConsumer<E, LivingEntity> onHurtByEntity, Predicate<E> canStartHunting){
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new OwnerHurtByTarget<>(SharedWolfAi::canDefendOwner, SharedWolfAi::wantsToAttack),
                        new OwnerHurtTarget<>(SharedWolfAi::canDefendOwner, SharedWolfAi::wantsToAttack),
                        new HurtByEntityTrigger<>(onHurtByEntity),
                        new StartAttacking<>(SharedWolfAi::canStartAttacking, SharedWolfAi::findNearestValidAttackTarget),
                        new StartStalking<>(SharedWolfBrain::canStalk, SharedWolfBrain::findNearestValidStalkTarget),
                        new StartHunting<>(canStartHunting, SharedWolfAi.TIME_BETWEEN_HUNTS),
                        new StopAttackingIfTargetInvalid<>(),
                        new StopBeingAngryIfTargetDead<>()
                ));
    }

    public static void onHurtBy(TamableAnimal tamableAnimal){
        SharedWolfAi.stopHoldingItemInMouth(tamableAnimal);
        SharedWolfAi.clearStates(tamableAnimal, true);
        tamableAnimal.setOrderedToSit(false);
    }

    private static boolean canStalk(TamableAnimal wolf){
        if(wolf.isBaby()){
            return SharedWolfAi.canMove(wolf);
        } else{
            return GenericAi.getNearbyVisibleAdults(wolf).isEmpty() && SharedWolfAi.canStartAttacking(wolf);
        }
    }

    private static Optional<? extends LivingEntity> findNearestValidStalkTarget(TamableAnimal wolf){
        if(wolf.isBaby()){
            return GenericAi.getNearestVisibleBabies(wolf).stream().filter(target -> canStalk(wolf, target)).findFirst();
        } else{
            return HunterAi.getNearestVisibleHuntable(wolf).filter(target -> canStalk(wolf, target));
        }
    }

    private static boolean canStalk(TamableAnimal wolf, LivingEntity target) {
        return wolf.distanceToSqr(target) > SharedWolfAi.POUNCE_DISTANCE && !AiUtil.isLookingAtMe(wolf, target, StalkPrey.INITIAL_VISION_OFFSET);
    }

    public static boolean canStartHunting(TamableAnimal wolf){
        return HunterAi.getStalkTarget(wolf).isEmpty() && SharedWolfAi.canStartAttacking(wolf);
    }

    public static <E extends TamableAnimal> ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> getUpdatePackage(List<Activity> activities, BiConsumer<E, Pair<Activity, Activity>> onActivityChanged){
        return BrainUtil.createPriorityPairs(99,
                ImmutableList.of(
                        new UpdateActivity<>(activities, onActivityChanged),
                        new UpdateTarget()
                ));
    }

    public static ImmutableList<? extends Pair<Integer, ? extends Behavior<? super TamableAnimal>>> getPanicPackage(){
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new Sprint<>(SharedWolfAi::canMove)
                ));
    }

    public static <E extends TamableAnimal> ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> getSitPackage(RunOne<TamableAnimal> idleLookBehaviors, Behavior<E> beg){
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new SitWhenOrderedTo(),
                        beg,
                        idleLookBehaviors
                ));
    }

    public static ImmutableList<? extends Pair<Integer, ? extends Behavior<? super TamableAnimal>>> getPouncePackage() {
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new Pounce(SharedWolfAi.POUNCE_DISTANCE, SharedWolfAi.POUNCE_HEIGHT)
                ));
    }

    public static ImmutableList<? extends Pair<Integer, ? extends Behavior<? super TamableAnimal>>> getStalkPackage() {
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new StalkPrey(SharedWolfAi.SPEED_MODIFIER_WALKING, SharedWolfAi.POUNCE_DISTANCE, SharedWolfAi.POUNCE_HEIGHT)
                ));
    }

    public static <E extends TamableAnimal> ImmutableList<? extends Pair<Integer, ? extends Behavior<? super E>>> getAvoidPackage(Predicate<E> stopAvoidingIf, RunOne<TamableAnimal> idleMovementBehaviors, RunOne<TamableAnimal> idleLookBehaviors) {
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new Sprint<>(SharedWolfAi::canMove),
                        SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, SharedWolfAi.SPEED_MODIFIER_RETREATING, SharedWolfAi.DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING, true),
                        idleMovementBehaviors,
                        idleLookBehaviors,
                        new EraseMemoryIf<>(stopAvoidingIf, MemoryModuleType.AVOID_TARGET)));
    }

    public static ImmutableList<? extends Pair<Integer, ? extends Behavior<? super TamableAnimal>>> getMeetPackage() {
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new Sprint<>(SharedWolfAi::canMove),
                        new StayCloseToTarget<>(SharedWolfAi::getHowlPosition, SharedWolfAi.ADULT_FOLLOW_RANGE.getMinValue() - 1, SharedWolfAi.ADULT_FOLLOW_RANGE.getMaxValue(), SharedWolfAi.SPEED_MODIFIER_WALKING),
                        new EraseMemoryIf<>(SharedWolfBrain::wantsToStopFollowingHowl, ABABMemoryModuleTypes.HOWL_LOCATION.get()))
        );
    }

    private static boolean wantsToStopFollowingHowl(TamableAnimal wolf){
        Optional<PositionTracker> howlPosition = SharedWolfAi.getHowlPosition(wolf);
        if (howlPosition.isEmpty()) {
            return true;
        } else {
            PositionTracker tracker = howlPosition.get();
            return wolf.position().closerThan(tracker.currentPosition(), SharedWolfAi.ADULT_FOLLOW_RANGE.getMaxValue());
        }
    }

    public static ImmutableList<? extends Pair<Integer, ? extends Behavior<? super TamableAnimal>>> getRestPackage(RunOne<TamableAnimal> idleLookBehaviors){
        return BrainUtil.createPriorityPairs(0,
                ImmutableList.of(
                        new SleepOnGround<>(SharedWolfAi::canSleep, SharedWolfAi::handleSleeping),
                        new RunIf<>(Predicate.not(LivingEntity::isSleeping), idleLookBehaviors, true)
                ));
    }

    public static Set<Pair<MemoryModuleType<?>, MemoryStatus>> getPanicConditions() {
        return ImmutableSet.of(Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT));
    }

    public static Set<Pair<MemoryModuleType<?>, MemoryStatus>> getSitConditions() {
        return ImmutableSet.of(Pair.of(ABABMemoryModuleTypes.IS_ORDERED_TO_SIT.get(), MemoryStatus.VALUE_PRESENT));
    }

    public static Set<Pair<MemoryModuleType<?>, MemoryStatus>> getRestConditions() {
        return ImmutableSet.of(
                Pair.of(ABABMemoryModuleTypes.IS_SHELTERED.get(), MemoryStatus.VALUE_PRESENT),
                Pair.of(ABABMemoryModuleTypes.IS_LEVEL_DAY.get(), MemoryStatus.VALUE_PRESENT),
                Pair.of(ABABMemoryModuleTypes.IS_ORDERED_TO_FOLLOW.get(), MemoryStatus.VALUE_ABSENT),
                Pair.of(ABABMemoryModuleTypes.IS_ORDERED_TO_HEEL.get(), MemoryStatus.VALUE_ABSENT));
    }

    public static boolean isFollowingOwner(TamableAnimal dog) {
        return CommandAi.isFollowing(dog) || CommandAi.isHeeling(dog);
    }

    public static void fetchItem(LivingEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        brain.eraseMemory(ABABMemoryModuleTypes.TIME_TRYING_TO_REACH_FETCH_ITEM.get());
        brain.setMemory(ABABMemoryModuleTypes.FETCHING_ITEM.get(), true);
    }
}
