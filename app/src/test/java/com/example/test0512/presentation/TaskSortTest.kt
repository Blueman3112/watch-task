package com.example.test0512.presentation

import com.example.test0512.model.RadarTask
import com.example.test0512.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortTest {

    @Test
    fun testPinnedTaskAlwaysFirst() {
        val now = System.currentTimeMillis()
        
        val veryUrgentTask = RadarTask(
            title = "Very Urgent",
            time = "Soon",
            dueDate = now + 1000, // Due in 1 second
            description = "",
            priority = TaskPriority.EMERGENCY,
            isPinned = false
        )
        
        val nonUrgentButPinnedTask = RadarTask(
            title = "Not Urgent but Pinned",
            time = "Later",
            dueDate = now + 86400_000L * 10, // Due in 10 days
            description = "",
            priority = TaskPriority.LONG_TERM,
            isPinned = true
        )

        val list = listOf(veryUrgentTask, nonUrgentButPinnedTask)

        val sortedList = list.sortedWith(TaskSorter.getComparator(now))

        // Assert that the pinned task is the absolute first item (index 0)
        assertEquals("Not Urgent but Pinned", sortedList[0].title)
        assertEquals("Very Urgent", sortedList[1].title)
    }

    @Test
    fun testMultiplePinnedTasksSortedByUrgency() {
        val now = System.currentTimeMillis()
        
        val pinnedUrgent = RadarTask(
            title = "Pinned Urgent",
            time = "Soon",
            dueDate = now + 1000,
            description = "",
            priority = TaskPriority.EMERGENCY,
            isPinned = true
        )
        
        val pinnedNonUrgent = RadarTask(
            title = "Pinned Non-Urgent",
            time = "Later",
            dueDate = now + 86400_000L * 10,
            description = "",
            priority = TaskPriority.LONG_TERM,
            isPinned = true
        )

        val unpinnedUrgent = RadarTask(
            title = "Unpinned Urgent",
            time = "Soon",
            dueDate = now + 1000,
            description = "",
            priority = TaskPriority.EMERGENCY,
            isPinned = false
        )

        val list = listOf(unpinnedUrgent, pinnedNonUrgent, pinnedUrgent)

        val sortedList = list.sortedWith(TaskSorter.getComparator(now))

        // Pinned ones first, sorted by urgency among themselves
        assertEquals("Pinned Urgent", sortedList[0].title)
        assertEquals("Pinned Non-Urgent", sortedList[1].title)
        assertEquals("Unpinned Urgent", sortedList[2].title)
    }
}
