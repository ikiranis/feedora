import { ref } from 'vue';

// On small screens the posts page hides the top bar and its toolbar while scrolling down, to show more posts
const hidden = ref(false);

export const mobileChromeStore = {
    hidden,
};
