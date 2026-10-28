import { ref, watch } from 'vue';

export type DateFormat = 'DD-MM-YYYY' | 'MM-DD-YYYY';

const STORAGE_KEY = 'date-format';

// Order of day and month in the dates shown on the site, persisted in localStorage (day first by default)
const format = ref<DateFormat>(localStorage.getItem(STORAGE_KEY) === 'MM-DD-YYYY' ? 'MM-DD-YYYY' : 'DD-MM-YYYY');

watch(format, (value) => localStorage.setItem(STORAGE_KEY, value));

const pad = (value: number) => String(value).padStart(2, '0');

const toDate = (date: Date | string | number) => date instanceof Date ? date : new Date(date);

/**
 * Day and month in the chosen order, e.g. 28-10 or 10-28
 */
const dayMonth = (date: Date) => format.value === 'DD-MM-YYYY'
    ? `${pad(date.getDate())}-${pad(date.getMonth() + 1)}`
    : `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

export const dateFormatStore = {
    format,

    /**
     * Time in 24 hour format, e.g. 18:05 or 18:05:42
     */
    time(date: Date | string | number, withSeconds = false): string {
        const d = toDate(date);
        const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
        return withSeconds ? `${time}:${pad(d.getSeconds())}` : time;
    },

    /**
     * Date with the year, e.g. 28-10-2026
     */
    date(date: Date | string | number): string {
        const d = toDate(date);
        return `${dayMonth(d)}-${d.getFullYear()}`;
    },

    /**
     * Date without the year, e.g. 28-10
     */
    shortDate(date: Date | string | number): string {
        return dayMonth(toDate(date));
    },

    /**
     * Date and time, e.g. 28-10-2026 18:05
     */
    dateTime(date: Date | string | number, withSeconds = false): string {
        return `${dateFormatStore.date(date)} ${dateFormatStore.time(date, withSeconds)}`;
    },
};
