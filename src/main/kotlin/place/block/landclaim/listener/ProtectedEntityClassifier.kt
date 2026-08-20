package place.block.landclaim.listener

import org.bukkit.entity.AbstractVillager
import org.bukkit.entity.Ambient
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.WaterMob
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame

object ProtectedEntityClassifier {
    fun isProtected(entity: Entity): Boolean {
        if (entity is ItemFrame || entity is ArmorStand) {
            return true
        }

        val livingEntity = entity as? LivingEntity ?: return false
        if (livingEntity is Player) {
            return false
        }

        if (livingEntity.customName() != null) {
            return true
        }

        return when (livingEntity) {
            is Animals -> true
            is AbstractVillager -> true
            is WaterMob -> true
            is Ambient -> true
            is Monster -> false
            else -> false
        }
    }
}
