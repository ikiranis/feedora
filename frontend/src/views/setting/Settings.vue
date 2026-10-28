<script setup lang="ts">
import {onMounted, Ref, ref} from "vue";
import Error from "@/components/error/Error.vue";
import {errorStore} from "@/components/error/errorStore.ts";
import {getSettings} from "@/api/setting.ts";
import router from "@/router";
import {language} from "@/functions/languageStore.ts";
import Page404 from "@/components/error/Page404.vue";
import Loading from "@/components/utilities/Loading.vue";
import LanguageSelect from "@/components/utilities/LanguageSelect.vue";
import {authStore} from "@/functions/authStore.ts";
import {dateFormatStore, DateFormat} from "@/functions/dateFormatStore.ts";

const dateFormats: DateFormat[] = ['DD-MM-YYYY', 'MM-DD-YYYY']

const settings: Ref<any[]> = ref([]);

const loading = ref(false)

onMounted(() => {
    // The app settings are only available to the admin
    if (authStore.isAdmin.value) {
        loadSettings()
    }
})

/**
 * Load all settings
 */
const loadSettings = async () => {
    loading.value = true

    await getSettings()
        .then(response => {
            settings.value = response
        })
        .catch((error) => {
            errorStore.set(true, error.response.data.message, error.response.data.status)
        })

    loading.value = false
}

const editSetting = (id: number) => {
    router.push({name: 'Setting', params: {id: id}})
}

</script>

<template>
    <!-- Display settings of the current user, kept in this browser -->
    <div class="my-3">
        <h5>{{ language.get('Display') || 'Display' }}</h5>

        <div class="d-flex align-items-center gap-3 mb-3">
            <span class="setting-label fw-semibold">{{ language.get('Language') || 'Language' }}</span>
            <language-select />
        </div>

        <div class="d-flex align-items-center gap-3">
            <span class="setting-label fw-semibold">{{ language.get('Date format') || 'Date format' }}</span>
            <div class="btn-group btn-group-sm" role="group" :aria-label="language.get('Date format') || 'Date format'">
                <button v-for="format in dateFormats" :key="format" type="button" class="btn btn-outline-secondary"
                        :class="{ active: dateFormatStore.format.value === format }"
                        :aria-pressed="dateFormatStore.format.value === format"
                        @click="dateFormatStore.format.value = format">
                    {{ format }}
                </button>
            </div>
        </div>
    </div>

    <template v-if="authStore.isAdmin.value">
    <h5 class="mt-4">{{ language.get('App Settings') }}</h5>

    <div v-if="loading" class="row">
        <Loading />
    </div>

    <div v-if="settings && settings.length > 0 && !loading" class="mb-3">
        <table class="table table-striped table-hover">
            <thead class="text-bg-dark">
            <tr>
                <th scope="col">#</th>
                <th scope="col">{{ language.get("Name") }}</th>
                <th scope="col">{{ language.get("Value") }}</th>
                <th scope="col"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="setting in settings">
                <td class="align-middle">
                    <!--                    <router-link class="idElement" :to="{ name: 'Document', params: { id: document.id } }">-->
                    {{ setting.id }}
                    <!--                    </router-link>-->
                </td>
                <td class="align-middle">
                    {{ setting.settingName }}
                </td>
                <td class="align-middle">
                    <!-- API keys are only shown when edited -->
                    {{ /apikey/i.test(setting.settingName) && setting.settingValue
                        ? '••••••••' + setting.settingValue.slice(-4) : setting.settingValue }}
                </td>
                <td class="my-auto align-middle">
                    <a class="btn btn-sm" @click="editSetting(setting.id)" :title="language.get('Edit Setting')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" fill="currentColor"
                             class="bi bi-pencil-square" viewBox="0 0 16 16">
                            <path
                                d="M15.502 1.94a.5.5 0 0 1 0 .706L14.459 3.69l-2-2L13.502.646a.5.5 0 0 1 .707 0l1.293 1.293zm-1.75 2.456-2-2L4.939 9.21a.5.5 0 0 0-.121.196l-.805 2.414a.25.25 0 0 0 .316.316l2.414-.805a.5.5 0 0 0 .196-.12l6.813-6.814z"/>
                            <path fill-rule="evenodd"
                                  d="M1 13.5A1.5 1.5 0 0 0 2.5 15h11a1.5 1.5 0 0 0 1.5-1.5v-6a.5.5 0 0 0-1 0v6a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5v-11a.5.5 0 0 1 .5-.5H9a.5.5 0 0 0 0-1H2.5A1.5 1.5 0 0 0 1 2.5v11z"/>
                        </svg>
                    </a>
                </td>
            </tr>
            </tbody>
        </table>

    </div>

    <Page404 v-if="!loading && settings.length === 0"  class="my-3"
             :title="language.get('Search problem')"
             :text="language.get('No Settings Founded')" />
    </template>

    <error />
</template>

<style scoped lang="scss">
.setting-label {
    min-width: 8rem;
}

// The language flags are made for the dark sidebar; outline the current one in the page's text color
:deep(.currentLanguage) {
    border-color: var(--bs-secondary-color);
}
</style>
