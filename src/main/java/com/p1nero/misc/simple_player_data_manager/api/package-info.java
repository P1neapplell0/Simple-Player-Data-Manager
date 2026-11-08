/**
 * Typed, namespaced player data API. Initialize schema keys on both physical sides during mod
 * construction. Reads use the current logical side's player instance; all writes require the
 * logical server thread. Values persist with the player and are mirrored only to the owning
 * client unless declared server-only. See the repository README for integration and examples.
 */
package com.p1nero.misc.simple_player_data_manager.api;
