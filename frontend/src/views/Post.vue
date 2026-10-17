<template>
    <div class="d-flex posts-layout">
        <aside class="folder-sidebar d-none d-md-block">
            <nav class="folder-sidebar-inner py-4">
                <div class="folder-item" :class="{ active: !showSaved && !selectedFolderId && !selectedFeedId }"
                    role="button" @click="selectAll" :title="language.get('All Posts') || 'All Posts'">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-collection flex-shrink-0" viewBox="0 0 16 16">
                        <path d="M2.5 3.5a.5.5 0 0 1 0-1h11a.5.5 0 0 1 0 1h-11zm2-2a.5.5 0 0 1 0-1h7a.5.5 0 0 1 0 1h-7zM0 13a1.5 1.5 0 0 0 1.5 1.5h13A1.5 1.5 0 0 0 16 13V6a1.5 1.5 0 0 0-1.5-1.5h-13A1.5 1.5 0 0 0 0 6v7zm1.5.5A.5.5 0 0 1 1 13V6a.5.5 0 0 1 .5-.5h13a.5.5 0 0 1 .5.5v7a.5.5 0 0 1-.5.5h-13z"/>
                    </svg>
                    <span class="folder-name">{{ language.get('All Posts') || 'All Posts' }}</span>
                    <span class="folder-count">{{ unreadCounts.total }}</span>
                </div>

                <div class="folder-item" :class="{ active: showSaved }"
                    role="button" @click="selectSaved" :title="language.get('Saved') || 'Saved'">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-bookmark flex-shrink-0" viewBox="0 0 16 16">
                        <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                    </svg>
                    <span class="folder-name">{{ language.get('Saved') || 'Saved' }}</span>
                    <span class="folder-count">{{ unreadCounts.saved }}</span>
                </div>

                <hr class="my-2" />

                <div class="folder-header">
                    <span class="folder-name">{{ language.get('Folders') || 'Folders' }}</span>
                    <button type="button" class="folder-toggle" @click="showEmptyFolders = !showEmptyFolders"
                        :title="showEmptyFolders
                            ? (language.get('Hide empty folders') || 'Hide empty folders')
                            : (language.get('Show empty folders') || 'Show empty folders')">
                        <svg v-if="showEmptyFolders" xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                            fill="currentColor" class="bi bi-eye" viewBox="0 0 16 16">
                            <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
                            <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
                        </svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                            fill="currentColor" class="bi bi-eye-slash" viewBox="0 0 16 16">
                            <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486l.708.709z"/>
                            <path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829l.822.822zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z"/>
                            <path d="M3.35 5.47c-.18.16-.353.322-.518.487A13.134 13.134 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7.029 7.029 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12-.708.708z"/>
                        </svg>
                    </button>
                    <button type="button" class="folder-toggle" :class="{ 'text-primary': editingFolders }"
                        @click="editingFolders = !editingFolders"
                        :title="editingFolders
                            ? (language.get('Finish editing folders') || 'Finish editing folders')
                            : (language.get('Edit folders') || 'Edit folders')">
                        <svg v-if="editingFolders" xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                            fill="currentColor" class="bi bi-check-lg" viewBox="0 0 16 16">
                            <path d="M12.736 3.97a.733.733 0 0 1 1.047 0c.286.289.29.756.01 1.05L7.88 12.01a.733.733 0 0 1-1.065.02L3.217 8.384a.757.757 0 0 1 0-1.06.733.733 0 0 1 1.047 0l3.052 3.093 5.4-6.425a.247.247 0 0 1 .02-.022z"/>
                        </svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                            fill="currentColor" class="bi bi-pencil" viewBox="0 0 16 16">
                            <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708L10.5 8.207l-3-3L12.146.146zM11.207 1.5 1.5 11.207V14.5h3.293L14.5 4.793 11.207 1.5z"/>
                        </svg>
                    </button>
                </div>

                <template v-for="folder in visibleFolders" :key="folder.id">
                    <div class="folder-item" :class="{ active: selectedFolderId === folder.id }"
                        role="button" @click="selectFolder(folder.id)" :title="folder.name">
                        <button type="button" class="folder-toggle" @click.stop="toggleFolder(folder.id)"
                            :title="expandedFolders.has(folder.id) ? language.get('Collapse') : language.get('Expand')">
                            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor"
                                class="bi bi-chevron-right folder-chevron" :class="{ expanded: expandedFolders.has(folder.id) }"
                                viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
                            </svg>
                        </button>
                        <span class="folder-name">{{ folder.name }}</span>
                        <template v-if="editingFolders">
                            <button type="button" class="folder-toggle" @click.stop="openEditFolder(folder)"
                                :title="language.get('Edit this folder') || 'Edit this folder'">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor"
                                    class="bi bi-pencil" viewBox="0 0 16 16">
                                    <path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708L10.5 8.207l-3-3L12.146.146zM11.207 1.5 1.5 11.207V14.5h3.293L14.5 4.793 11.207 1.5z"/>
                                </svg>
                            </button>
                            <button type="button" class="folder-toggle text-danger" @click.stop="removeFolder(folder)"
                                :title="language.get('Delete this folder') || 'Delete this folder'">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor"
                                    class="bi bi-trash" viewBox="0 0 16 16">
                                    <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5Zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6Z"/>
                                    <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1ZM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118ZM2.5 3h11V2h-11v1Z"/>
                                </svg>
                            </button>
                        </template>
                        <span v-else class="folder-count">{{ unreadCounts.folders[folder.id] || 0 }}</span>
                    </div>

                    <template v-if="expandedFolders.has(folder.id)">
                        <button v-for="feed in visibleFeedsByFolder[folder.id] || []" :key="feed.id" type="button"
                            class="folder-item feed-item" :class="{ active: selectedFeedId === feed.id }"
                            @click="selectFeed(feed.id)" :title="feed.title">
                            <img v-if="getFeedIcon(feed)" :src="getFeedIcon(feed)!" class="feed-icon flex-shrink-0"
                                alt="" loading="lazy" @error="failedFeedIcons.add(feed.id)" />
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-rss flex-shrink-0" viewBox="0 0 16 16">
                                <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z"/>
                                <path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1zm0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1z"/>
                            </svg>
                            <span class="folder-name">{{ feed.title }}</span>
                            <span class="folder-count">{{ feed.count }}</span>
                        </button>
                    </template>
                </template>

                <!-- Feeds that are not in any folder -->
                <button v-for="feed in visibleOrphanFeeds" :key="feed.id" type="button"
                    class="folder-item orphan-feed-item" :class="{ active: selectedFeedId === feed.id }"
                    @click="selectFeed(feed.id)" :title="feed.title">
                    <img v-if="getFeedIcon(feed)" :src="getFeedIcon(feed)!" class="feed-icon flex-shrink-0"
                        alt="" loading="lazy" @error="failedFeedIcons.add(feed.id)" />
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-rss flex-shrink-0" viewBox="0 0 16 16">
                        <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h12zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2H2z"/>
                        <path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1zm0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 0-4-4 1 1 0 0 1-1-1z"/>
                    </svg>
                    <span class="folder-name">{{ feed.title }}</span>
                    <span class="folder-count">{{ feed.count }}</span>
                </button>

                <button type="button" class="folder-item text-secondary mt-2" @click="openAddFolder"
                    :title="language.get('Add a new folder to organize your feeds') || 'Add a new folder to organize your feeds'">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-plus-lg flex-shrink-0" viewBox="0 0 16 16">
                        <path fill-rule="evenodd" d="M8 2a.5.5 0 0 1 .5.5v5h5a.5.5 0 0 1 0 1h-5v5a.5.5 0 0 1-1 0v-5h-5a.5.5 0 0 1 0-1h5v-5A.5.5 0 0 1 8 2Z"/>
                    </svg>
                    <span class="folder-name">{{ language.get('Add Folder') || 'Add Folder' }}</span>
                </button>
            </nav>
        </aside>

        <div class="flex-grow-1 posts-main">
            <div class="container pt-2 pb-0">
                <div class="row justify-content-end mb-1 align-items-center">
                    <div v-if="selectedFeedTitle" class="col feed-title-col">
                        <div class="dropdown d-flex align-items-center">
                            <button type="button" class="btn btn-link text-reset text-decoration-none p-0 d-flex align-items-center gap-2 mw-100"
                                data-bs-toggle="dropdown" aria-expanded="false" :title="selectedFeedTitle">
                                <h6 class="mb-0 text-truncate">{{ selectedFeedTitle }}</h6>
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" class="bi bi-chevron-down flex-shrink-0" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z"/></svg>
                            </button>
                            <ul class="dropdown-menu">
                                <li>
                                    <button type="button" class="dropdown-item" @click="openEditFeed">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-pencil me-2" viewBox="0 0 16 16"><path d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5v.5h.293zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325"/></svg>{{ language.get('Edit Feed') || 'Edit Feed' }}
                                    </button>
                                </li>
                                <li>
                                    <button type="button" class="dropdown-item" @click="openFeedStatistics">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-bar-chart me-2" viewBox="0 0 16 16"><path d="M4 11H2v3h2zm5-4H7v7h2zm5-5v12h-2V2zm-2-1a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1zM6 7a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1zm-5 4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1z"/></svg>{{ language.get('Statistics') || 'Statistics' }}
                                    </button>
                                </li>
                                <li><hr class="dropdown-divider"></li>
                                <li>
                                    <button type="button" class="dropdown-item text-danger" @click="unfollowFeed">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-x-circle me-2" viewBox="0 0 16 16"><path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/><path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708"/></svg>{{ language.get('Unfollow feed') || 'Unfollow feed' }}
                                    </button>
                                </li>
                            </ul>
                        </div>
                    </div>
                    <div class="col-auto d-flex align-items-center">
                        <div v-if="!showSaved" class="btn-group btn-group-sm me-2" role="group"
                            :aria-label="language.get('Posts to show') || 'Posts to show'">
                            <button type="button" class="btn btn-outline-secondary" :class="{ active: !showRead }"
                                @click="showRead = false" :disabled="loading"
                                :title="language.get('Show only unread posts') || 'Show only unread posts'">
                                {{ language.get('Unread') || 'Unread' }}
                            </button>
                            <button type="button" class="btn btn-outline-secondary" :class="{ active: showRead }"
                                @click="showRead = true" :disabled="loading"
                                :title="language.get('Show all posts') || 'Show all posts'">
                                {{ language.get('All') || 'All' }}
                            </button>
                        </div>
                        <div v-if="!showSaved" class="btn-group btn-group-sm me-2">
                            <button type="button" class="btn btn-outline-primary d-flex align-items-center gap-2"
                                @click="handleMarkAllAsRead(null)" :disabled="loading"
                                :title="language.get('Mark all as read') || 'Mark all as read'">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                    class="bi bi-check2-all" viewBox="0 0 16 16">
                                    <path d="M12.354 4.354a.5.5 0 0 0-.708-.708L5 10.293 1.854 7.146a.5.5 0 1 0-.708.708l3.5 3.5a.5.5 0 0 0 .708 0l7-7zm-4.208 7-.896-.897.707-.707.543.543 6.646-6.647a.5.5 0 0 1 .708.708l-7 7a.5.5 0 0 1-.708 0z"/>
                                    <path d="m5.354 7.146.896.897-.707.707-.897-.896a.5.5 0 1 1 .708-.708z"/>
                                </svg>
                                <span class="d-none d-xl-inline">{{ language.get('Mark all as read') || 'Mark all as read' }}</span>
                            </button>
                            <button ref="markAsReadToggleRef" type="button" class="btn btn-outline-primary dropdown-toggle dropdown-toggle-split"
                                data-bs-toggle="dropdown" aria-expanded="false" :disabled="loading">
                                <span class="visually-hidden">{{ language.get('Mark as read') || 'Mark as read' }}</span>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <li><h6 class="dropdown-header">{{ language.get('Mark as read') || 'Mark as read' }}</h6></li>
                                <li v-for="option in markAsReadOptions" :key="option.label">
                                    <button type="button" class="dropdown-item" @click="handleMarkAllAsRead(option.days)">
                                        {{ language.get(option.label) || option.label }}
                                    </button>
                                </li>
                            </ul>
                        </div>
                        <button @click="handleRefreshPosts" class="btn btn-sm btn-outline-primary me-2"
                            :title="language.get('Refresh Posts') || 'Refresh Posts'"
                            :disabled="loading">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" 
                                class="bi bi-arrow-clockwise" :class="{ 'spin': loading }" viewBox="0 0 16 16">
                                <title>{{ language.get('Refresh Posts') || 'Refresh Posts' }}</title>
                                <path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                                <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
                            </svg>
                        </button>
                        <button @click="showDetails = !showDetails" class="btn btn-sm btn-outline-secondary me-2"
                            :title="showDetails ? language.get('Show Summary') : language.get('Show Details')">
                            <svg v-if="showDetails" xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                fill="currentColor" class="bi bi-list" viewBox="0 0 16 16">
                                <title>{{ language.get('Show Summary') }}</title>
                                <path fill-rule="evenodd"
                                    d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5zm0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5z" />
                            </svg>
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-card-text" viewBox="0 0 16 16">
                                <title>{{ language.get('Show Details') }}</title>
                                <path
                                    d="M14.5 3a.5.5 0 0 1 .5.5v9a.5.5 0 0 1-.5.5h-13a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h13zm-13-1A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h13a1.5 1.5 0 0 0 1.5-1.5v-9A1.5 1.5 0 0 0 14.5 2h-13z" />
                                <path
                                    d="M3 5.5a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5zM3 8a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9A.5.5 0 0 1 3 8zm0 2.5a.5.5 0 0 1 .5-.5h6a.5.5 0 0 1 0 1h-6a.5.5 0 0 1-.5-.5z" />
                            </svg>
                        </button>
                        <button v-if="!showSaved && currentFolderId" type="button" class="btn btn-sm btn-outline-secondary me-2"
                            :class="{ active: hideShorts }" @click="hideShorts = !hideShorts" :disabled="loading"
                            :title="hideShorts
                                ? (language.get('Show YouTube Shorts in this folder') || 'Show YouTube Shorts in this folder')
                                : (language.get('Hide YouTube Shorts in this folder') || 'Hide YouTube Shorts in this folder')">
                            <svg v-if="hideShorts" xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                fill="currentColor" class="bi bi-eye-slash" viewBox="0 0 16 16">
                                <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486l.708.709z"/>
                                <path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829l.822.822zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z"/>
                                <path d="M3.35 5.47c-.18.16-.353.322-.518.487A13.134 13.134 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7.029 7.029 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12-.708.708z"/>
                            </svg>
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                fill="currentColor" class="bi bi-eye" viewBox="0 0 16 16">
                                <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
                                <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
                            </svg>
                        </button>
                        <button @click="handleParseFeeds" class="btn btn-sm btn-outline-primary me-2"
                            :title="language.get('Parse RSS Feeds') || 'Parse RSS Feeds'" :disabled="loading">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-cloud-arrow-down" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M7.646 10.854a.5.5 0 0 0 .708 0l2-2a.5.5 0 0 0-.708-.708L8.5 9.293V5.5a.5.5 0 0 0-1 0v3.793L6.354 8.146a.5.5 0 1 0-.708.708l2 2z"/>
                                <path d="M4.406 3.342A5.53 5.53 0 0 1 8 2c2.69 0 4.923 2 5.166 4.579C14.758 6.804 16 8.137 16 9.773 16 11.569 14.502 13 12.687 13H3.781C1.708 13 0 11.366 0 9.318c0-1.763 1.266-3.223 2.942-3.593.143-.863.698-1.723 1.464-2.383zm.653.757c-.757.653-1.153 1.44-1.153 2.056v.448l-.445.049C2.064 6.805 1 7.952 1 9.318 1 10.785 2.23 12 3.781 12h8.906C13.98 12 15 10.988 15 9.773c0-1.216-1.02-2.228-2.313-2.228h-.5v-.5C12.188 4.825 10.328 3 8 3a4.53 4.53 0 0 0-2.941 1.1z"/>
                            </svg>
                        </button>
                        <div class="input-group input-group-sm" style="width: 220px;">
                            <span class="input-group-text">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                                    <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                                </svg>
                            </span>
                            <input 
                                v-model="searchTerm"
                                type="text" 
                                class="form-control"
                                :placeholder="language.get('Search posts by title or description...')"
                                :title="language.get('Filter posts by title or description')"
                            />
                        </div>
                    </div>
                </div>
                <div class="row justify-content-end">
                    <div class="col-auto">
                        <small class="text-muted last-parsed d-block" :title="lastParsedAt ? new Date(lastParsedAt).toLocaleString() : ''">
                            {{ language.get('Last parsing') || 'Last parsing' }}: {{ lastParsedLabel }}
                        </small>
                    </div>
                </div>
            </div>
            
            <div class="scrollable-post-view" ref="scrollableContainerRef">
                <!-- Posts of the current view added by a parse since the list was loaded -->
                <div v-if="newPostsCount > 0" class="new-posts-badge-wrapper">
                    <button type="button" class="btn btn-sm btn-primary rounded-pill shadow-sm new-posts-badge d-flex align-items-center gap-1"
                        @click="showNewPosts" :disabled="loading"
                        :title="language.get('Show the new posts') || 'Show the new posts'">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" class="bi bi-arrow-up" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M8 15a.5.5 0 0 0 .5-.5V2.707l3.146 3.147a.5.5 0 0 0 .708-.708l-4-4a.5.5 0 0 0-.708 0l-4 4a.5.5 0 1 0 .708.708L7.5 2.707V14.5a.5.5 0 0 0 .5.5"/>
                        </svg>
                        {{ newPostsCount }} {{ newPostsCount === 1 ? (language.get('new post') || 'new post') : (language.get('new posts') || 'new posts') }}
                    </button>
                </div>
                <div class="container pt-1 pb-4">
                    <div v-if="posts.length > 0" class="row" :class="showDetails ? 'g-4' : 'g-0'">
                        <div v-for="(post, index) in posts" :key="post.id" :data-post-id="post.id"
                            :class="showDetails ? 'col-12 col-md-6 col-lg-4' : 'col-12'"
                            @click="selectedIndex = !showDetails && selectedIndex === index ? -1 : index">
                            <PostElement :post="post" :show-details="showDetails" :selected="index === selectedIndex"
                                @post-read="handlePostRead" @toggle-saved="toggleSaved" @toggle-read="toggleRead" @show-logs="showPostLogs"
                                @show-feed="showFeed" @close="selectedIndex = -1" @open="selectedIndex = index; modalOpen = true" />
                        </div>
                        <div v-if="allLoaded && posts.length > 0 && !searchTerm.trim()" class="text-center w-100 text-muted my-4">
                            {{ language.get('No more posts') || 'No more posts' }}
                        </div>
                    </div>
                    <div v-else-if="!loading && posts.length === 0 && !searchTerm.trim()" class="text-center text-muted mt-5">
                        {{ showSaved ? (language.get('No saved posts') || 'No saved posts') : language.get('No posts found') }}
                    </div>
                    <div v-else-if="!loading && posts.length === 0 && searchTerm.trim()" class="text-center text-muted mt-5">
                        {{ language.get('No posts match your search') || 'No posts match your search' }}
                    </div>

                    <!-- Fixed bottom-right loading spinner, not shown while parsing since the scan progress bar shows it -->
                    <div v-if="loading && !scanStore.active.value" class="loading-spinner-fixed">
                        <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <PostModal v-if="modalPost" ref="postModalRef" :post="modalPost"
        @close="modalOpen = false" @toggle-saved="toggleSaved" @show-logs="showPostLogs" @show-feed="showFeed" />

    <LogsModal :post-id="logsPost?.id" :feed-id="logsPost?.feed?.id" :title="logsPost?.title" :show="!!logsPost"
        @logs-cleared="handlePostLogsCleared" @modal-hidden="logsPost = null" />

    <KeyboardShortcutsModal :show="showShortcuts" @modal-hidden="showShortcuts = false" />

    <FeedStatisticsModal :feed="statisticsFeed" :show="!!statisticsFeed" @modal-hidden="statisticsFeed = null" />
    <EditFeedModal :feed="editedFeed" :show="!!editedFeed" @feed-updated="fetchFolders(); fetchUnreadCounts()" @modal-hidden="editedFeed = null" />

    <AddFolderModal @folder-added="fetchFolders" />
    <EditFolderModal :folder="editedFolder" @folder-updated="fetchFolders" />

    <Error class="error-fixed-bottom" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getPosts, parseFeeds, markAsRead, markAsUnread, markAllAsRead, getUnreadCounts, setPostSaved, countNewPosts } from '@/api/post';
import { getFeedOperationStatus, getLastParsedAt, getFeeds, deleteFeed } from '@/api/feed';
import { getFolders, deleteFolder } from '@/api/folder';
import { PostType, FolderType, UnreadCountsType, FeedUnreadCountType, Feed } from '@/types';
import { language } from '@/functions/languageStore';
import { postContentStore } from '@/functions/postContentStore';
import PostElement from '@/components/PostElement.vue';
import PostModal from '@/components/PostModal.vue';
import LogsModal from '@/components/log/LogsModal.vue';
import KeyboardShortcutsModal from '@/components/KeyboardShortcutsModal.vue';
import { clipboardStore } from '@/functions/clipboardStore';
import AddFolderModal from '@/components/folder/AddFolderModal.vue';
import FeedStatisticsModal from '@/components/feed/FeedStatisticsModal.vue';
import EditFeedModal from '@/components/feed/EditFeedModal.vue';
import EditFolderModal from '@/components/folder/EditFolderModal.vue';
import Error from '@/components/error/Error.vue'; // Changed from ErrorDisplay
import { errorStore } from '@/components/error/errorStore'; // Import errorStore
import { scanStore } from '@/functions/scanStore';
import { confirmDialog } from '@/functions/javaDialogs';
import { Dropdown, Modal } from 'bootstrap';

const route = useRoute();
const router = useRouter();
const posts = ref<PostType[]>([]);
const page = ref(1);
const pageSize = 50;
const loading = ref(false);
const allLoaded = ref(false);
// Server time of the first page's listing. Marking all as read only marks the posts added up to it, not the ones parsed since.
const listedAt = ref<string | null>(null);
const newPostsCount = ref(0); // Unread posts of the current view added since listedAt, e.g. by a scheduled parse
const lastParsedAt = ref<string | null>(null); // When the feeds were last parsed, by the scheduler or manually
let lastParsedTimer: number | null = null;
const scrollableContainerRef = ref<HTMLElement | null>(null); // Ref for the scrollable container
const intersectionObserver = ref<IntersectionObserver | null>(null);
const observedPosts = ref<Set<string>>(new Set()); // Unread posts currently visible in the scrollable container
const lastScrollTop = ref(0); // Scroll position at the previous scroll event, to detect scrolling down
const searchTerm = ref('');
const searchTimeout = ref<number | null>(null);
let skipSearchWatch = false; // Set when the search is cleared by a filter change, to avoid a second fetch
const selectedIndex = ref(-1); // Index of the card selected via keyboard navigation
const modalOpen = ref(false); // The selected post is shown in full in a modal
const postModalRef = ref<InstanceType<typeof PostModal> | null>(null);
const modalPost = computed(() => modalOpen.value ? posts.value[selectedIndex.value] ?? null : null);
const logsPost = ref<PostType | null>(null); // The post whose log entries are shown in the logs modal
const showShortcuts = ref(false); // The help modal with the keyboard shortcuts is shown
const folders = ref<FolderType[]>([]);
const unreadCounts = ref<UnreadCountsType>({ total: 0, saved: 0, folders: {}, feeds: [] });
const selectedFolderId = ref<string | null>(null); // null shows the posts of all folders
const selectedFeedId = ref<string | null>(null); // null shows the posts of all feeds
const showSaved = ref(false); // Shows the saved posts (read or unread) instead of the unread ones
const expandedFolders = ref<Set<string>>(new Set());
const failedFeedIcons = ref<Set<string>>(new Set());
const showEmptyFolders = ref(localStorage.getItem('posts-show-empty-folders') === 'true'); // Persisted in localStorage
const editingFolders = ref(false); // Shows the edit/delete buttons of the folders in the sidebar
const editedFolder = ref<FolderType | null>(null); // Folder shown in the edit folder modal
const statisticsFeed = ref<Feed | null>(null); // Feed shown in the statistics modal
const editedFeed = ref<Feed | null>(null); // Feed shown in the edit feed modal
const showRead = ref(localStorage.getItem('posts-show-read') === 'true'); // Lists the read posts too, persisted in localStorage

// Folders whose YouTube Shorts are hidden, persisted in localStorage. Feeds use their folder's setting,
// and the "All Posts" view leaves out the Shorts of these folders too.
const HIDE_SHORTS_KEY = 'posts-hide-shorts-folders';
const hiddenShortsFolders = ref<Set<string>>((() => {
    try {
        return new Set<string>(JSON.parse(localStorage.getItem(HIDE_SHORTS_KEY) || '[]'));
    } catch {
        return new Set<string>();
    }
})());

// Folder of the current view (the selected folder, or the folder of the selected feed), null for the other views
const currentFolderId = computed<string | null>(() => selectedFolderId.value
    ?? unreadCounts.value.feeds.find(f => f.id === selectedFeedId.value)?.folderId
    ?? null);

// Whether the Shorts of the current folder are hidden. Changing it reloads the posts and the unread counts.
const hideShorts = computed<boolean>({
    get: () => !!currentFolderId.value && hiddenShortsFolders.value.has(currentFolderId.value),
    set: (value) => {
        const folderId = currentFolderId.value;
        if (!folderId) return;
        const folderIds = new Set(hiddenShortsFolders.value);
        if (value) folderIds.add(folderId);
        else folderIds.delete(folderId);
        hiddenShortsFolders.value = folderIds;
        localStorage.setItem(HIDE_SHORTS_KEY, JSON.stringify([...folderIds]));
        scrollableContainerRef.value?.scrollTo({ top: 0 });
        handleRefreshPosts();
    },
});

// Choices of the "Mark all as read" dropdown, by the age of the posts to mark (null marks all of them)
const markAsReadToggleRef = ref<HTMLElement | null>(null);
const markAsReadOptions: { label: string; days: number | null }[] = [
    { label: 'All posts', days: null },
    { label: 'Older than 1 day', days: 1 },
    { label: 'Older than 3 days', days: 3 },
    { label: 'Older than 1 week', days: 7 },
    { label: 'Older than 2 weeks', days: 14 },
    { label: 'Older than 1 month', days: 30 },
];

// Cards (true) or list (false) mode of each view (all, saved, a folder or a feed), persisted in localStorage
const VIEW_MODES_KEY = 'posts-view-modes';
const defaultShowDetails = localStorage.getItem('posts-show-details') !== 'false'; // Legacy global setting
const viewModes = ref<Record<string, boolean>>((() => {
    try {
        return JSON.parse(localStorage.getItem(VIEW_MODES_KEY) || '{}');
    } catch {
        return {};
    }
})());

// Key of the view mode for the current filter
const viewModeKey = computed(() => {
    if (showSaved.value) return 'saved';
    if (selectedFeedId.value) return `feed:${selectedFeedId.value}`;
    if (selectedFolderId.value) return `folder:${selectedFolderId.value}`;
    return 'all';
});

// Mode of the current view. A feed without its own mode uses its folder's one, then list mode.
const showDetails = computed<boolean>({
    get: () => {
        const own = viewModes.value[viewModeKey.value];
        if (own !== undefined) return own;
        if (selectedFeedId.value) {
            const folderId = unreadCounts.value.feeds.find(f => f.id === selectedFeedId.value)?.folderId;
            const folderMode = folderId ? viewModes.value[`folder:${folderId}`] : undefined;
            if (folderMode !== undefined) return folderMode;
        }
        // Folders and feeds default to list mode
        if (selectedFeedId.value || selectedFolderId.value) return false;
        return defaultShowDetails;
    },
    set: (value) => {
        viewModes.value = { ...viewModes.value, [viewModeKey.value]: value };
        localStorage.setItem(VIEW_MODES_KEY, JSON.stringify(viewModes.value));
    },
});

// Title of the selected feed, shown above the posts list
const selectedFeedTitle = computed(() => unreadCounts.value.feeds.find(f => f.id === selectedFeedId.value)?.title ?? null);

// Folders with unread posts, sorted by name. The selected folder (or the folder of the selected feed)
// stays visible after its last post is read.
const visibleFolders = computed(() => {
    const selectedFeedFolderId = unreadCounts.value.feeds.find(f => f.id === selectedFeedId.value)?.folderId;
    return folders.value
        .filter(folder => showEmptyFolders.value
            || editingFolders.value
            || (unreadCounts.value.folders[folder.id] || 0) > 0
            || folder.id === selectedFolderId.value
            || folder.id === selectedFeedFolderId)
        .sort((a, b) => a.name.localeCompare(b.name));
});

// Feeds with unread posts (or all feeds when empty folders are shown) grouped by folder id and sorted by title.
// The selected feed stays visible after its last post is read.
const visibleFeedsByFolder = computed(() => {
    const groups: Record<string, FeedUnreadCountType[]> = {};
    for (const feed of unreadCounts.value.feeds) {
        if (!feed.folderId) continue;
        if (feed.count <= 0 && !showEmptyFolders.value && feed.id !== selectedFeedId.value) continue;
        (groups[feed.folderId] ||= []).push(feed);
    }
    Object.values(groups).forEach(feeds => feeds.sort((a, b) => a.title.localeCompare(b.title)));
    return groups;
});

// Feeds that are not in any folder, shown under the folders with the same visibility rules, sorted by title
const visibleOrphanFeeds = computed(() => {
    const folderIds = new Set(folders.value.map(folder => folder.id));
    return unreadCounts.value.feeds
        .filter(feed => !feed.folderId || !folderIds.has(feed.folderId))
        .filter(feed => feed.count > 0 || showEmptyFolders.value || feed.id === selectedFeedId.value)
        .sort((a, b) => a.title.localeCompare(b.title));
});

/**
 * Favicon URL of the feed's website, or null if it has none or it failed to load.
 */
const getFeedIcon = (feed: FeedUnreadCountType): string | null => {
    if (!feed.htmlUrl || failedFeedIcons.value.has(feed.id)) return null;
    try {
        return `https://www.google.com/s2/favicons?domain=${new URL(feed.htmlUrl).hostname}&sz=32`;
    } catch {
        return null;
    }
};

/**
 * Expands or collapses the feed list of a folder in the sidebar.
 */
const toggleFolder = (folderId: string) => {
    if (expandedFolders.value.has(folderId)) {
        expandedFolders.value.delete(folderId);
    } else {
        expandedFolders.value.add(folderId);
    }
};

/**
 * Fetches the user's folders for the sidebar.
 */
const fetchFolders = async () => {
    try {
        folders.value = await getFolders();
    } catch (e) {
        console.error('Error fetching folders:', e);
        folders.value = [];
    }
};

/**
 * Fetches the selected feed, since the sidebar only has its title and counts. Null if it can't be fetched.
 */
const fetchSelectedFeed = async (): Promise<Feed | null> => {
    try {
        const feeds: Feed[] = await getFeeds();
        return feeds.find(feed => feed.id === selectedFeedId.value) || null;
    } catch (e: any) {
        errorStore.set(true, e.response?.data || language.get('Failed to fetch feeds') || 'Failed to fetch feeds.', e.response?.status || 500);
        return null;
    }
};

/**
 * Opens the statistics modal of the selected feed. The feed is fetched on demand, to show up to date statistics.
 */
const openFeedStatistics = async () => {
    statisticsFeed.value = await fetchSelectedFeed();
};

/**
 * Opens the edit feed modal of the selected feed.
 */
const openEditFeed = async () => {
    editedFeed.value = await fetchSelectedFeed();
};

/**
 * Unfollows (deletes) the selected feed after confirmation, then shows the posts of all feeds.
 */
const unfollowFeed = async () => {
    if (!confirmDialog(`${language.get('Are you sure you want to delete this feed') || 'Are you sure you want to delete this feed'}\n\n${selectedFeedTitle.value}`)) {
        return;
    }
    try {
        const result = await deleteFeed(selectedFeedId.value!);
        await fetchUnreadCounts();
        selectAll();
        errorStore.set(true, result, 200); // Show success message
    } catch (e: any) {
        errorStore.set(true, e.response?.data || language.get('Failed to delete feed') || 'Failed to delete feed.', e.response?.status || 500);
    }
};

/**
 * Opens the modal that creates a new folder.
 */
const openAddFolder = () => {
    const modalElement = document.getElementById('addFolderModal');
    if (modalElement) Modal.getOrCreateInstance(modalElement).show();
};

/**
 * Opens the modal that renames the given folder.
 */
const openEditFolder = (folder: FolderType) => {
    editedFolder.value = folder;
    const modalElement = document.getElementById('editFolderModal');
    if (modalElement) Modal.getOrCreateInstance(modalElement).show();
};

/**
 * Deletes a folder after confirmation. Its feeds are moved out of the folder by the server.
 */
const removeFolder = async (folder: FolderType) => {
    const folderFeeds = unreadCounts.value.feeds.filter(feed => feed.folderId === folder.id);
    const feedCount = folderFeeds.length;
    // The deleted folder (or one of its feeds) is being shown
    const isShown = selectedFolderId.value === folder.id || folderFeeds.some(feed => feed.id === selectedFeedId.value);
    let message = `${language.get('Are you sure you want to delete this folder') || 'Are you sure you want to delete this folder'}?\n\n"${folder.name}"`;
    if (feedCount > 0) {
        message += `\n\n${language.get('This folder contains') || 'This folder contains'} ${feedCount} ${language.get('feeds') || 'feeds'}. ${language.get('These feeds will be moved to the default folder') || 'These feeds will be moved to the default folder'}.`;
    }
    if (!confirmDialog(message)) return;

    try {
        const result = await deleteFolder(folder.id);
        await Promise.all([fetchFolders(), fetchUnreadCounts()]);
        if (isShown) selectAll();
        errorStore.set(true, result, 200);
    } catch (e: any) {
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to delete folder') || 'Failed to delete folder.', 500);
        }
    }
};

/**
 * Fetches the unread post counts shown next to each folder in the sidebar.
 */
const fetchUnreadCounts = async () => {
    try {
        unreadCounts.value = await getUnreadCounts([...hiddenShortsFolders.value]);
    } catch (e) {
        console.error('Error fetching unread counts:', e);
    }
};

/**
 * Changes the folder/feed/saved filter of the posts list and reloads it.
 * Selecting the current filter again also reloads it, to fetch the latest posts.
 */
const applyFilter = (folderId: string | null, feedId: string | null, saved = false) => {
    selectedFolderId.value = folderId;
    selectedFeedId.value = feedId;
    showSaved.value = saved;
    if (searchTerm.value) {
        // The posts are reloaded below, so the search watcher must not fetch them again
        skipSearchWatch = true;
        searchTerm.value = '';
    }
    scrollableContainerRef.value?.scrollTo({ top: 0 });
    handleRefreshPosts();
};

const selectAll = () => applyFilter(null, null);
const selectFolder = (folderId: string) => applyFilter(folderId, null);
const selectFeed = (feedId: string) => applyFilter(null, feedId);
const selectSaved = () => applyFilter(null, null, true);

/**
 * Shows the posts of a post's feed, closing the opened post.
 */
const showFeed = (feedId: string) => {
    modalOpen.value = false;
    selectedIndex.value = -1;
    selectFeed(feedId);
};

/**
 * Fetches posts from the API. It supports pagination and resetting the post list.
 * It also processes post descriptions to remove image size attributes.
 * @param {boolean} [reset=false] - If true, clears existing posts and resets pagination.
 */
const fetchPosts = async (reset = false) => {
    try {
        if (reset) {
            posts.value = [];
            page.value = 1;
            allLoaded.value = false;
            selectedIndex.value = -1;
        }

        const { posts: data, listedAt: pageListedAt } = await getPosts(page.value, pageSize, searchTerm.value, selectedFolderId.value, selectedFeedId.value, showSaved.value, showRead.value,
            [...hiddenShortsFolders.value]);

        if (data.length < pageSize) {
            allLoaded.value = true;
        }

        if (reset) {
            posts.value = data;
            listedAt.value = pageListedAt;
            newPostsCount.value = 0;
        } else {
            posts.value = posts.value.concat(data);
        }

        page.value++; // Increment page for the next fetch

        // Re-observe post elements after posts are loaded, and keep loading while the list
        // is too short to scroll, since infinite scroll only triggers on scroll events
        nextTick(() => {
            observePostElements();
            handleScroll();
        });
    } catch (e: any) {
        console.error('Error fetching posts:', e);
        if (e.response && e.response.data) {
            errorStore.set(true, e.response.data.message || e.response.data, e.response.status || 500);
        } else {
            errorStore.set(true, language.get('Failed to fetch posts') || 'Failed to fetch posts.', 500);
        }
    } finally {
        loading.value = false;
    }
};

/**
 * Handles the action of parsing RSS feeds. It calls the parseFeeds API for the current view
 * (the selected feed, the selected folder, or all feeds), then refreshes the post list and shows an alert with the result.
 */
const handleParseFeeds = async () => {
    try {
        // Check if a feed operation is already running
        const statusResponse = await getFeedOperationStatus();
        
        if (statusResponse.isRunning) {
            errorStore.set(true, language.get('Cannot parse feeds: Another feed operation is currently in progress. Please try again in a few moments.') || 'Cannot parse feeds: Another feed operation is currently in progress. Please try again in a few moments.', 409);
            return;
        }
    } catch (e: any) {
        console.warn('Could not check feed operation status:', e);
    }

    try {
        loading.value = true;
        scanStore.active.value = true;
        const result = await parseFeeds(selectedFolderId.value, selectedFeedId.value);
        await fetchPosts(true); // Refresh posts
        fetchUnreadCounts();
        fetchLastParsedAt();
        errorStore.set(true, result, 200); // Use errorStore for success
    } catch (e: any) { // Added :any to e for response access
        if (e.response && e.response.data && e.response.data.message && typeof e.response.data.status === 'number') {
            errorStore.set(true, e.response.data.message, e.response.data.status);
        } else {
            errorStore.set(true, language.get('Failed to parse feeds') || 'Failed to parse feeds.', 500);
        }
    } finally {
        scanStore.active.value = false;
        loading.value = false;
    }
};

/**
 * Marks all the unread posts of the current view (all, a folder or a feed) as read, then reloads the list.
 * Posts added after the list was loaded (e.g. by a parse in the meantime) stay unread.
 * @param olderThanDays - Only mark the posts older than this many days, or null for all of them.
 *                        Marking all of them from the main button asks for confirmation first.
 */
const handleMarkAllAsRead = async (olderThanDays: number | null) => {
    // Closed explicitly, since Bootstrap doesn't close a dropdown whose toggle got disabled (by loading) during the click
    if (markAsReadToggleRef.value) {
        Dropdown.getOrCreateInstance(markAsReadToggleRef.value).hide();
    }
    if (olderThanDays === null && !confirmDialog(language.get('Mark all posts of this view as read?') || 'Mark all posts of this view as read?')) {
        return;
    }
    try {
        loading.value = true;
        await markAllAsRead(selectedFolderId.value, selectedFeedId.value, olderThanDays, listedAt.value);
        scrollableContainerRef.value?.scrollTo({ top: 0 });
        await handleRefreshPosts();
    } catch (e: any) {
        console.error('Failed to mark posts as read:', e);
        loading.value = false;
        errorStore.set(true, e.response?.data?.message || language.get('Failed to mark posts as read') || 'Failed to mark posts as read.', e.response?.status || 500);
    }
};

/**
 * Counts the unread posts of the current view added since the list was loaded, for the new posts badge.
 * The saved view isn't counted, since parsed posts aren't saved.
 */
const fetchNewPostsCount = async () => {
    if (showSaved.value || !listedAt.value) {
        newPostsCount.value = 0;
        return;
    }
    const view = { listedAt: listedAt.value, folderId: selectedFolderId.value, feedId: selectedFeedId.value };
    try {
        const count = await countNewPosts(view.listedAt, view.folderId, view.feedId, [...hiddenShortsFolders.value]);
        // Ignored if the view changed or was reloaded in the meantime
        if (view.listedAt === listedAt.value && view.folderId === selectedFolderId.value && view.feedId === selectedFeedId.value) {
            newPostsCount.value = count;
        }
    } catch (e) {
        console.warn('Could not count the new posts:', e);
    }
};

/**
 * Reloads the list from the top to show the new posts.
 */
const showNewPosts = () => {
    scrollableContainerRef.value?.scrollTo({ top: 0 });
    handleRefreshPosts();
};

/**
 * Fetches when the feeds were last parsed. Failures are ignored, the label just keeps its previous value.
 */
const fetchLastParsedAt = async () => {
    try {
        lastParsedAt.value = await getLastParsedAt();
    } catch (e) {
        console.warn('Could not fetch the last parse time:', e);
    }
};

// Time only for today, date and time otherwise
const lastParsedLabel = computed(() => {
    if (!lastParsedAt.value) {
        return language.get('Never') || 'Never';
    }
    const date = new Date(lastParsedAt.value);
    return date.toDateString() === new Date().toDateString()
        ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleString([], { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
});

/**
 * Handles refreshing the posts by fetching the latest data.
 */
const handleRefreshPosts = async () => {
    loading.value = true;
    fetchLastParsedAt();
    // Clean up current intersection observer
    cleanupIntersectionObserver();
    fetchUnreadCounts();
    await fetchPosts(true); // Reset and fetch fresh posts
    // Re-setup intersection observer after posts are loaded
    nextTick(() => {
        setupIntersectionObserver();
    });
};

/**
 * Handles when a post is marked as read.
 * Updates the local state to reflect the change immediately.
 */
const handlePostRead = (postId: string) => {
    const postIndex = posts.value.findIndex(post => post.id === postId);
    if (postIndex !== -1) {
        const post = posts.value[postIndex];
        if (!post.read) {
            decrementUnreadCount(post);
        }
        post.read = true;
        // Remove from observed posts since it's now read
        observedPosts.value.delete(postId);
        
        // Stop observing this post element since it's now read
        if (intersectionObserver.value && scrollableContainerRef.value) {
            const postElement = scrollableContainerRef.value.querySelector(`[data-post-id="${postId}"]`);
            if (postElement) {
                intersectionObserver.value.unobserve(postElement);
            }
        }
    }
};

/**
 * Lowers the sidebar unread counts after a post has been marked as read.
 */
const decrementUnreadCount = (post: PostType) => {
    unreadCounts.value.total = Math.max(0, unreadCounts.value.total - 1);
    const folderId = post.feed?.folderId;
    if (folderId && unreadCounts.value.folders[folderId]) {
        unreadCounts.value.folders[folderId]--;
    }
    const feed = unreadCounts.value.feeds.find(f => f.id === post.feed?.id);
    if (feed && feed.count > 0) {
        feed.count--;
    }
};

/**
 * Marks a post as read via API call.
 * Used when posts are scrolled past.
 */
const markPostAsReadAPI = async (postId: string) => {
    try {
        await markAsRead(postId);
        handlePostRead(postId); // Update local state
    } catch (error) {
        console.error('Failed to mark post as read:', error);
    }
};

/**
 * Toggles the read state of a post from its read indicator.
 * A post marked as unread isn't observed again, so scrolling doesn't mark it as read right away.
 */
const toggleRead = async (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (!post) return;
    if (!post.read) {
        await markPostAsReadAPI(postId);
        return;
    }
    try {
        await markAsUnread(postId);
        post.read = false;
        incrementUnreadCount(post);
    } catch (e: any) {
        console.error('Failed to mark post as unread:', e);
        errorStore.set(true, language.get('Failed to mark post as unread') || 'Failed to mark post as unread.', e.response?.status || 500);
    }
};

/**
 * Raises the sidebar unread counts after a post has been marked as unread.
 */
const incrementUnreadCount = (post: PostType) => {
    unreadCounts.value.total++;
    const folderId = post.feed?.folderId;
    if (folderId) {
        unreadCounts.value.folders[folderId] = (unreadCounts.value.folders[folderId] || 0) + 1;
    }
    const feed = unreadCounts.value.feeds.find(f => f.id === post.feed?.id);
    if (feed) {
        feed.count++;
    }
};

/**
 * Saves a post for later, or removes it from the saved posts.
 * In the saved view the post stays listed until the next refresh, so it can be saved again.
 */
const toggleSaved = async (postId: string) => {
    const post = posts.value.find(p => p.id === postId);
    if (!post) return;
    const saved = !post.saved;
    try {
        await setPostSaved(postId, saved);
        post.saved = saved;
        unreadCounts.value.saved = Math.max(0, unreadCounts.value.saved + (saved ? 1 : -1));
    } catch (e: any) {
        console.error('Failed to save post:', e);
        errorStore.set(true, e.response?.data?.message || language.get('Failed to save post') || 'Failed to save post.', e.response?.status || 500);
    }
};

/**
 * Sets up intersection observer to track which unread posts are currently visible.
 * It doesn't mark posts as read itself: markScrolledPastPosts does, on scroll events, so posts moved
 * out of view by layout changes (e.g. a post expanding or collapsing) stay unread.
 */
const setupIntersectionObserver = () => {
    if (!scrollableContainerRef.value) return;

    intersectionObserver.value = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                const postElement = entry.target as HTMLElement;
                const postId = postElement.getAttribute('data-post-id');
                
                if (!postId) return;

                const post = posts.value.find(p => p.id === postId);
                if (!post || post.read) return;

                if (entry.isIntersecting) {
                    observedPosts.value.add(postId);
                } else {
                    observedPosts.value.delete(postId);
                }
            });
        },
        {
            root: scrollableContainerRef.value,
            rootMargin: '0px',
            threshold: 0 // Trigger when post is completely out of view
        }
    );

    // Observe all current post elements
    observePostElements();
};

/**
 * Observes all unread post elements for intersection changes.
 */
const observePostElements = () => {
    if (!intersectionObserver.value || !scrollableContainerRef.value) return;

    const postElements = scrollableContainerRef.value.querySelectorAll('[data-post-id]');
    postElements.forEach((element) => {
        const postId = element.getAttribute('data-post-id');
        if (postId) {
            const post = posts.value.find(p => p.id === postId);
            // Only observe unread posts
            if (post && !post.read) {
                intersectionObserver.value!.observe(element);
            }
        }
    });
};

/**
 * Cleans up intersection observer.
 */
const cleanupIntersectionObserver = () => {
    if (intersectionObserver.value) {
        intersectionObserver.value.disconnect();
        intersectionObserver.value = null;
    }
    observedPosts.value.clear();
};

/**
 * Marks as read the visible unread posts that a downward scroll has moved completely above the top of the container,
 * i.e. the posts the user has scrolled past and doesn't see anymore.
 */
const markScrolledPastPosts = () => {
    const container = scrollableContainerRef.value;
    if (!container) return;

    const scrollTop = container.scrollTop;
    const isScrollingDown = scrollTop > lastScrollTop.value;
    lastScrollTop.value = scrollTop;
    if (!isScrollingDown) return;

    const containerTop = container.getBoundingClientRect().top;
    Array.from(observedPosts.value).forEach((postId) => {
        const element = container.querySelector(`[data-post-id="${postId}"]`);
        if (element && element.getBoundingClientRect().bottom <= containerTop) {
            observedPosts.value.delete(postId);
            markPostAsReadAPI(postId);
        }
    });
};

/**
 * Handles the scroll event on the scrollable container.
 * When the user scrolls near the bottom, it loads more posts if available.
 */
const handleScroll = () => {
    if (loading.value || allLoaded.value || !scrollableContainerRef.value) return;

    const container = scrollableContainerRef.value;
    const scrollTop = container.scrollTop;
    const clientHeight = container.clientHeight;
    const scrollHeight = container.scrollHeight;

    // Load more when 200px from the bottom
    if (scrollTop + clientHeight + 200 >= scrollHeight) {
        loading.value = true;
        fetchPosts(); // fetchPosts advances the page itself
    }
};

/**
 * Returns the number of cards per row by counting the cards rendered on the same line as the first one.
 */
const getColumnCount = (): number => {
    if (!scrollableContainerRef.value) return 1;
    const elements = Array.from(scrollableContainerRef.value.querySelectorAll<HTMLElement>('[data-post-id]'));
    if (elements.length === 0) return 1;
    const firstTop = elements[0].offsetTop;
    return Math.max(1, elements.filter(el => el.offsetTop === firstTop).length);
};

/**
 * Selects the card at the given index, scrolls it into view and marks it as read.
 */
const selectPost = (index: number) => {
    if (index < 0 || index >= posts.value.length) return;
    selectedIndex.value = index;

    const post = posts.value[index];
    // List mode scrolls the opened post to its start (see the selectedIndex watcher)
    if (showDetails.value) {
        nextTick(() => {
            const element = scrollableContainerRef.value?.querySelector(`[data-post-id="${post.id}"]`);
            element?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        });
    }

    if (!post.read) {
        markPostAsReadAPI(post.id);
    }
};

/**
 * Opens a link in a background tab. Uses the Feedora Companion extension (see /extension) when installed,
 * otherwise simulates a Cmd/Ctrl+click, which browsers may open as a regular new tab instead.
 */
const openInBackgroundTab = (url: string) => {
    if (document.documentElement.dataset.feedoraExtension) {
        window.postMessage({ type: 'FEEDORA_OPEN_BACKGROUND_TAB', url }, window.location.origin);
        return;
    }

    const isMac = /Mac|iPhone|iPad/.test(navigator.platform);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.target = '_blank';
    anchor.rel = 'noopener noreferrer';
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        cancelable: true,
        view: window,
        ctrlKey: !isMac,
        metaKey: isMac,
    }));
    anchor.remove();
};

/**
 * Opens the modal with the log entries of a post, e.g. why its content couldn't be retrieved.
 */
const showPostLogs = (post: PostType) => {
    logsPost.value = post;
};

/**
 * Hides the logs icon of the post whose log entries were deleted.
 */
const handlePostLogsCleared = () => {
    if (logsPost.value) {
        postContentStore.clearLogCount(logsPost.value.id);
    }
};

/**
 * Returns the pressed key, with letters lowercased. When the keyboard layout doesn't produce a Latin letter
 * (e.g. Greek), the letter of the physical key is used instead, so the letter shortcuts work with any layout.
 */
const getShortcutKey = (event: KeyboardEvent): string => {
    if (/^[a-z]$/i.test(event.key)) return event.key.toLowerCase();
    if (/^Key[A-Z]$/.test(event.code)) return event.code.slice(3).toLowerCase();
    return event.key;
};

/**
 * Handles arrow key navigation between cards.
 * In list mode, Up/Down scroll the list instead (so long expanded posts can be read) and Left/Right move between posts.
 * Enter or O opens the selected post in a modal (Esc closes it), B opens its link in a background tab, F saves it for later (or unsaves it),
 * C copies its link to the clipboard.
 * H shows the help modal with all the keyboard shortcuts.
 */
const handleKeydown = (event: KeyboardEvent) => {
    // The logs modal handles its own keys (Esc closes it)
    if (logsPost.value) return;
    const target = event.target as HTMLElement | null;
    if (target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))) return;
    if (event.altKey || event.ctrlKey || event.metaKey) return;

    const key = getShortcutKey(event);
    if (key === 'h') {
        showShortcuts.value = !showShortcuts.value;
        event.preventDefault();
        return;
    }
    // The help modal handles its own keys (Esc closes it)
    if (showShortcuts.value || posts.value.length === 0) return;

    const current = selectedIndex.value;
    const columns = getColumnCount();
    let next: number;

    switch (key) {
        case 'ArrowRight':
            next = current < 0 ? 0 : current + 1;
            break;
        case 'ArrowLeft':
            next = current < 0 ? 0 : current - 1;
            break;
        case 'ArrowDown':
        case 'ArrowUp':
            if (modalOpen.value) {
                postModalRef.value?.scrollBy(key === 'ArrowDown' ? 80 : -80);
                event.preventDefault();
                return;
            }
            if (!showDetails.value) {
                scrollableContainerRef.value?.scrollBy({ top: key === 'ArrowDown' ? 80 : -80 });
                event.preventDefault();
                return;
            }
            next = current < 0
                ? 0
                : key === 'ArrowDown' ? Math.min(current + columns, posts.value.length - 1) : current - columns;
            break;
        case 'Enter':
        case 'o':
            if (current >= 0 && posts.value[current]) {
                modalOpen.value = !modalOpen.value;
                event.preventDefault();
            }
            return;
        case 'Escape':
            // Close the modal, or else collapse the post opened in list mode
            if (modalOpen.value) modalOpen.value = false;
            else if (!showDetails.value) selectedIndex.value = -1;
            return;
        case 'b':
            if (current >= 0 && posts.value[current]?.link) {
                openInBackgroundTab(posts.value[current].link);
                event.preventDefault();
            }
            return;
        case 'f':
            if (current >= 0 && posts.value[current]) {
                toggleSaved(posts.value[current].id);
                event.preventDefault();
            }
            return;
        case 'c':
            if (current >= 0 && posts.value[current]?.link) {
                clipboardStore.copy(posts.value[current].link);
                event.preventDefault();
            }
            return;
        default:
            return;
    }

    event.preventDefault();
    selectPost(next);
};

// In list mode, start reading the opened post from its top instead of keeping the previous post's scroll position
watch(selectedIndex, (index) => {
    if (showDetails.value || index < 0) return;
    const post = posts.value[index];
    if (!post) return;
    // Wait for the previous post to collapse and this one to expand before measuring
    nextTick(() => {
        const container = scrollableContainerRef.value;
        const element = container?.querySelector<HTMLElement>(`[data-post-id="${post.id}"]`);
        if (!container || !element) return;
        const top = element.getBoundingClientRect().top - container.getBoundingClientRect().top + container.scrollTop;
        container.scrollTo({ top });
    });
});

// Close the modal when no post is selected anymore (e.g. the posts were reloaded)
watch(modalPost, (post) => {
    if (!post) modalOpen.value = false;
});

// In list mode, preload the content of the selected post and the next ones (or the first ones if none is selected),
// so opening them shows the full article right away
const PREFETCH_COUNT = 4;
watch([selectedIndex, showDetails, () => posts.value.length], () => {
    if (showDetails.value) return;
    const start = Math.max(0, selectedIndex.value);
    postContentStore.prefetch(posts.value.slice(start, start + PREFETCH_COUNT));
});

// Persist the visibility of folders without unread posts
watch(showEmptyFolders, (value) => {
    localStorage.setItem('posts-show-empty-folders', String(value));
});

// Persist the unread/all posts choice and reload the list with it
watch(showRead, (value) => {
    localStorage.setItem('posts-show-read', String(value));
    scrollableContainerRef.value?.scrollTo({ top: 0 });
    handleRefreshPosts();
});

// Watch for search term changes with debouncing
watch(searchTerm, (_) => {
    // Clear existing timeout
    if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
    }

    // The search was cleared by a filter change, which already reloads the posts
    if (skipSearchWatch) {
        skipSearchWatch = false;
        return;
    }

    // Set new timeout for debounced search
    searchTimeout.value = setTimeout(() => {
        // Cleanup intersection observer before search
        cleanupIntersectionObserver();
        
        // Reset and fetch with search
        loading.value = true;
        fetchPosts(true);
        
        // Re-setup intersection observer after search
        nextTick(() => {
            setupIntersectionObserver();
        });
    }, 500); // 500ms debounce delay
});

// A scan (scheduled or manual) ended, so the counts, the new posts badge and the last parse time are refreshed
watch(scanStore.finishedCount, () => {
    fetchUnreadCounts();
    fetchNewPostsCount();
    fetchLastParsedAt();
});

// Lifecycle hooks
onMounted(() => {
    // Clear any existing errors when navigating to posts page
    errorStore.clear();

    // A feed selected from the feeds page. The query is removed, so it doesn't override later filter changes.
    if (typeof route.query.feed === 'string') {
        selectedFeedId.value = route.query.feed;
        router.replace({ query: {} });
    }

    loading.value = true;
    fetchFolders();
    fetchUnreadCounts();
    fetchPosts(true); // Initial fetch
    fetchLastParsedAt();
    // The scheduler parses in the background, so the time is refreshed every minute
    lastParsedTimer = window.setInterval(fetchLastParsedAt, 60 * 1000);
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.addEventListener('scroll', handleScroll);
        scrollableContainerRef.value.addEventListener('scroll', markScrolledPastPosts);
    }
    // Setup intersection observer for marking posts as read when scrolled past
    nextTick(() => {
        setupIntersectionObserver();
    });
    window.addEventListener('keydown', handleKeydown);
    // Ensure body overflow is hidden when this component is active
    document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
    if (scrollableContainerRef.value) {
        scrollableContainerRef.value.removeEventListener('scroll', handleScroll);
        scrollableContainerRef.value.removeEventListener('scroll', markScrolledPastPosts);
    }
    window.removeEventListener('keydown', handleKeydown);
    if (lastParsedTimer) {
        clearInterval(lastParsedTimer);
    }
    // Cleanup search timeout
    if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
    }
    // Cleanup intersection observer
    cleanupIntersectionObserver();
    // Restore body overflow when this component is destroyed
    document.body.style.overflow = 'auto';
});

</script>

<style>
/* Make the main view scrollable instead of the whole page */
.scrollable-post-view {
    height: calc(100vh - 110px); /* Viewport minus top bar and posts toolbar */
    overflow-y: auto;
}

/* New posts badge, floating over the top of the posts list while it scrolls */
.new-posts-badge-wrapper {
    position: sticky;
    top: 0;
    height: 0;
    z-index: 10;
    display: flex;
    justify-content: center;
    overflow: visible;
}

.new-posts-badge {
    margin-top: 0.5rem;
    font-size: 0.8rem;
}

/* Folder sidebar on the left of the posts list */
.last-parsed {
    font-size: 0.7rem;
    line-height: 1.2;
}

.posts-main {
    min-width: 0;
}

.folder-sidebar {
    position: relative;
    width: 240px;
    flex-shrink: 0;
    border-right: 1px solid var(--bs-border-color);
}

/* Absolutely positioned so the sidebar takes the height of the posts column and scrolls on its own */
.folder-sidebar-inner {
    position: absolute;
    inset: 0;
    overflow-y: auto;
    padding-right: 0.75rem;
}

.folder-item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    width: 100%;
    padding: 0.5rem 0.75rem;
    margin-bottom: 2px;
    border: 0;
    border-radius: 0.5rem;
    background: transparent;
    color: var(--bs-body-color);
    text-align: left;
    cursor: pointer;
}

.folder-item:hover {
    background-color: var(--bs-tertiary-bg);
}

.folder-item.active {
    background-color: var(--bs-secondary-bg);
    color: var(--bs-primary);
    font-weight: 600;
}

/* "Folders" header with the edit toggle */
.folder-header {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.25rem 0.75rem;
    font-size: 0.8em;
    font-weight: 600;
    text-transform: uppercase;
    color: var(--bs-secondary-color);
}

/* Chevron that expands a folder to show its feeds */
.folder-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    flex-shrink: 0;
    padding: 0;
    border: 0;
    border-radius: 0.25rem;
    background: transparent;
    color: inherit;
}

.folder-toggle:hover {
    background-color: var(--bs-secondary-bg);
}

.folder-chevron {
    transition: transform 0.15s ease;
}

.folder-chevron.expanded {
    transform: rotate(90deg);
}

/* Lets the selected feed's title shrink and truncate next to the toolbar */
.feed-title-col {
    min-width: 0;
}

.feed-item {
    padding-left: 2.25rem;
    font-size: 0.95em;
}

/* Feed outside any folder, at the same level as the folders */
.orphan-feed-item {
    font-size: 0.95em;
}

.feed-icon {
    width: 16px;
    height: 16px;
    border-radius: 3px;
}

.folder-name {
    flex-grow: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.folder-count {
    font-size: 0.85em;
    color: var(--bs-secondary-color);
}

/* Ensure images in post descriptions fit the card width */
.post-img {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
}

/* Fixed bottom-right loading spinner */
.loading-spinner-fixed {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 1000;
    /* Ensure it's above other content */
}

/* Fixed bottom-left error component */
.error-fixed-bottom {
    position: fixed;
    bottom: 5em;
    left: 20px;
    z-index: 1000; /* Same z-index as spinner, or adjust as needed */
}

/* Refresh button spin animation */
.spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}
</style>
