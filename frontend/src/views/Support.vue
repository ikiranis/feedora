<script setup lang="ts">
import {language} from "@/functions/languageStore.ts"

/** Mail address feedback and support requests are sent to. */
const supportEmail = "rocean74@gmail.com"

/** Where bug reports and feature requests can be filed as a GitHub issue instead of by mail. */
const githubIssuesUrl = "https://github.com/ikiranis/feedora/issues"

/**
 * PayPal's classic donation link. It takes only the recipient's account email, so no
 * PayPal-dashboard-hosted button id needs to be created or kept in sync with this app.
 * No `amount` is passed: PayPal fixes the amount as read-only whenever one is given in the
 * URL, so leaving it out is what lets the donor type any value. `no_recurring=0` lets them
 * pick a one-off or a monthly donation on PayPal's own page.
 */
const paypalDonateUrl = `https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=${encodeURIComponent(supportEmail)}&currency_code=EUR&no_recurring=0`
</script>

<template>
	<div class="container-fluid mt-4 support">
		<h4 class="mb-1">{{ language.get("Support") }}</h4>

		<p class="support__author">{{ language.get("Created by apps4net - Yiannis Kiranis") }}</p>

		<section class="support__section">
			<h5>{{ language.get("Contact") }}</h5>
			<p>{{ language.get("Found a bug, or have an idea for Feedora? Send feedback by mail, or open an issue on GitHub.") }}</p>
			<div class="support__contact-links">
				<a class="support__mail" :href="`mailto:${supportEmail}`">
					<svg xmlns="http://www.w3.org/2000/svg" width="18" fill="currentColor"
						 class="bi bi-envelope-fill" viewBox="0 0 16 16">
						<path d="M.05 3.555A2 2 0 0 1 2 2h12a2 2 0 0 1 1.95 1.555L8 8.414zM0 4.697v7.104l5.803-3.558zM6.761 8.83l-6.57 4.027A2 2 0 0 0 2 14h12a2 2 0 0 0 1.808-1.144l-6.57-4.027L8 9.586zm3.436-.586L16 11.801V4.697z"/>
					</svg>
					<span>{{ supportEmail }}</span>
				</a>

				<a class="support__github" :href="githubIssuesUrl" target="_blank" rel="noopener noreferrer">
					<svg xmlns="http://www.w3.org/2000/svg" width="18" fill="currentColor"
						 class="bi bi-github" viewBox="0 0 16 16">
						<path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8"/>
					</svg>
					<span>{{ language.get("Open an issue on GitHub") }}</span>
				</a>
			</div>
		</section>

		<section class="support__section">
			<h5>{{ language.get("Donate") }}</h5>
			<p>{{ language.get("If Feedora is useful to you, consider supporting its development with a donation.") }}</p>
			<a class="support__paypal" :href="paypalDonateUrl" target="_blank" rel="noopener noreferrer"
			   :title="language.get('Donate with PayPal')">
				<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 16 16" fill="currentColor">
					<path d="M6.5 3h4.2c1.9 0 3.1 1 2.8 2.8-.3 2.1-1.8 3.3-3.9 3.3H8.1c-.3 0-.5.2-.6.5l-.6 3.6c0 .2-.2.3-.4.3H4.9c-.2 0-.3-.2-.3-.4l1.5-9.7c0-.2.2-.4.4-.4z"/>
				</svg>
				<span>{{ language.get("Donate with PayPal") }}</span>
			</a>
		</section>
	</div>
</template>

<style scoped lang="scss">
.support {
	max-width: 860px;

	&__author {
		margin-bottom: 2rem;
		color: var(--bs-secondary-color);
		font-size: 0.95rem;
	}

	&__section {
		margin-bottom: 2rem;

		h5 {
			margin-bottom: 0.75rem;
		}

		p {
			margin-bottom: 0.75rem;
		}
	}

	&__contact-links {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 1.5rem;
	}

	&__mail,
	&__github {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		text-decoration: none;
	}

	&__paypal {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.55rem 1.1rem;
		border-radius: 24px;
		background-color: #ffc439;
		color: #111;
		font-weight: 700;
		text-decoration: none;
		transition: background-color 0.2s;

		&:hover {
			background-color: #f5b700;
			color: #111;
		}
	}
}
</style>
