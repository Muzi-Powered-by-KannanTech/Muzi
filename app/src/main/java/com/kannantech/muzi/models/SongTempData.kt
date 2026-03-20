/*
 * Copyright (C) 2025 MUZI Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.kannantech.muzi.models

import com.kannantech.muzi.db.entities.FormatEntity
import com.kannantech.muzi.db.entities.Song

/**
 * For passing along song metadata
 */
data class SongTempData(val song: Song, val format: FormatEntity?)
