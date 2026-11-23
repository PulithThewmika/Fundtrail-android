# Accessibility

FundTrail is meant to be usable by everyone who manages their own money, including people who use a screen reader, larger text or a high-contrast setup. This statement describes what we aim for, what the app supports today, what we know is missing, and how to tell us about a barrier.

## Our commitment

Accessibility is a written requirement of the app, not an afterthought. Requirement **NFR-15** in the SRS asks for:

- **TalkBack labels** on every control, including icon-only buttons
- **Text contrast** of at least 4.5:1, in both light and dark themes
- **Touch targets** of at least 48 dp
- Screens that stay **usable at 200% font scale**, with no money figure cut off

We also follow these principles from the design:

- **Calm, neutral language.** No guilt, shame or alarm wording (NFR-14).
- **Meaning never relies on colour alone.** Red is not used for ordinary spending (NFR-16).
- **No time pressure.** Nothing in the app times out while you are entering data.

## What the app supports

| Area | Support |
|---|---|
| Screen readers | Icon buttons, chips and images carry TalkBack descriptions. Section titles are marked as headings so you can jump between them. Loading placeholders are announced as one labelled item instead of many empty shapes. |
| Text size | All text uses scalable `sp` units and follows the system font size. |
| Themes | Full light and dark themes that follow the system setting. |
| Touch | Interactive controls target at least 48 dp. |
| App lock | While the app is locked, its content is removed from the accessibility tree, so TalkBack announces only the lock screen and its *Unlock* button. Unlocking can use the phone's PIN, pattern or password, not only biometrics. |
| Entry | Quick add opens with the amount focused and the numeric keypad showing, so a basic expense takes three steps. |

## Supported environments

| | |
|---|---|
| Platform | Android 8.0 (API 26) and later |
| Devices | Phones in portrait, 360–480 dp wide |
| Assistive technology | TalkBack, Android font size and display size settings, dark theme |

Tablets, landscape layouts, iOS and the web are not supported.

## Known limitations

- **The Insights trend chart has no screen-reader description yet.** TalkBack users can't read the trend lines; the current month's figures are available as text on the dashboard.
- **No formal audit.** The accessibility checks in our regression plan (cases REG-A11Y-01 to 05) were defined but their results were not recorded, so we can't claim full NFR-15 compliance yet.
- **Switch Access and external keyboards** have not been tested.
- **Home-screen widget** content depends on the launcher's own accessibility support.

## Reporting a barrier

If something in FundTrail is hard or impossible to use with your setup, please [open an issue](https://github.com/PulithThewmika/Fundtrail-android/issues/new/choose) using the **Bug** template and add the word **Accessibility** to the title.

It helps us to know:

- the screen and what you were trying to do
- your device, Android version and any assistive technology or settings you use (for example TalkBack, 200% font size)
- what happened, and what you expected

Accessibility problems that stop someone from recording or reading their money are treated as high priority. FundTrail is maintained by a small team, so fixes are best effort, but we will reply to every report.

## More information

- Requirement NFR-15 and the related design rules: [`Docs/SRS.pdf`](Docs/SRS.pdf)
- How the app is designed and tested: the [FundTrail Wiki](https://github.com/PulithThewmika/Fundtrail-android/wiki)
