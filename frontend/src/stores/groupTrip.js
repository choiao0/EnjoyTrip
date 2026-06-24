import { defineStore } from 'pinia'
import { ref } from 'vue'
import { groupTripApi } from '../api/index.js'

export const useGroupTripStore = defineStore('groupTrip', () => {
  const groups = ref([])
  const currentGroup = ref(null)
  const members = ref([])
  const places = ref([])
  const isMember = ref(false)
  const isHost = ref(false)

  async function fetchGroups() {
    const res = await groupTripApi.list()
    groups.value = res.data
  }

  async function fetchDetail(id) {
    currentGroup.value = null
    members.value = []
    places.value = []
    const res = await groupTripApi.detail(id)
    currentGroup.value = res.data.group
    members.value = res.data.members
    places.value = res.data.places
    isMember.value = res.data.isMember
    isHost.value = res.data.isHost
  }

  async function createGroup(title, description) {
    const res = await groupTripApi.create({ title, description })
    groups.value.unshift(res.data)
    return res.data
  }

  async function deleteGroup(id) {
    await groupTripApi.delete(id)
    groups.value = groups.value.filter(g => g.id !== id)
  }

  async function joinGroup(id) {
    const res = await groupTripApi.join(id)
    members.value.push(res.data)
    isMember.value = true
  }

  async function leaveGroup(id) {
    await groupTripApi.leave(id)
    isMember.value = false
  }

  async function kickMember(groupId, userId) {
    await groupTripApi.kickMember(groupId, userId)
    members.value = members.value.filter(m => m.userId !== userId)
  }

  async function addPlace(groupId, place) {
    const res = await groupTripApi.addPlace(groupId, place)
    const exists = places.value.some(p => p.id === res.data.id)
    if (!exists) places.value.push(res.data)
    return res.data
  }

  async function removePlace(groupId, placeId) {
    await groupTripApi.removePlace(groupId, placeId)
    places.value = places.value.filter(p => p.id !== placeId)
  }

  // WebSocket 이벤트 핸들러
  function handleWsEvent(event) {
    if (event.type === 'PLACE_ADDED') {
      const exists = places.value.some(p => p.id === event.data.id)
      if (!exists) places.value.push(event.data)
    } else if (event.type === 'PLACE_REMOVED') {
      places.value = places.value.filter(p => p.id !== event.data.placeId)
    } else if (event.type === 'MEMBER_JOINED') {
      const exists = members.value.some(m => m.userId === event.data.userId)
      if (!exists) members.value.push(event.data)
    } else if (event.type === 'MEMBER_LEFT') {
      members.value = members.value.filter(m => m.userId !== event.data.userId)
    }
  }

  return {
    groups, currentGroup, members, places, isMember, isHost,
    fetchGroups, fetchDetail, createGroup, deleteGroup,
    joinGroup, leaveGroup, kickMember, addPlace, removePlace,
    handleWsEvent
  }
})
