# UI Roadmap & Implementation Notes

We will implement these items one-by-one. Priority for tomorrow is at the bottom.

✅ 1. ADD APP BRANDING
- What: Add logo + small tagline in the top app bar / splash.
- Visual:
  - [ Wallet Logo ]
  - Credixa
  - Smart Payments Simplified
- Implementation notes:
  - Add vector drawable for the logo (res/drawable/ic_wallet_logo.xml).
  - Use a small, centered title + subtitle under the logo on launch/splash.
  - Add app icon assets (adaptive launcher icons).

✅ 2. USE CARD-STYLE INPUTS
- Current: plain EditText
- Goal: modern filled inputs with rounded corners and subtle elevation.
- Implementation notes:
  - Use Material `TextInputLayout` + `TextInputEditText` with `boxBackgroundMode="filled"`.
  - Use `shapeAppearance` to set corner radius (12-16dp).
  - Add subtle elevation via `android:translationZ` on container card if needed.

✅ 3. BACKGROUND COLOR
- Replace flat gray with soft fintech background:
  - Preferred: `#F7F5FA` or `#FAFAFC`
- Implementation:
  - Set in theme as `windowBackground` and `colorBackground`.

✅ 4. PRIMARY COLOR (theme)
- Use consistent theme:
  - Primary: `#6C4AB6`
  - Dark Primary: `#4F2D96`
  - Background: `#FAFAFC`
  - Text: `#1E1E1E`
  - Hint Text: `#9E9E9E`
- Implementation notes:
  - Update `colors.xml` and `themes.xml` (MaterialComponents).
  - Use `colorPrimary`, `colorPrimaryVariant`, `colorOnPrimary`.

✅ 5. BUTTON DESIGN
- Improve buttons: rounded gradient, elevation, ripple.
- Design:
  - Rounded radius: 18dp
  - Height: 52dp
  - Bold text
- Implementation notes:
  - Use `MaterialButton` with custom style and `shapeAppearance`.
  - Apply a left-to-right gradient drawable for background.
  - Add elevation and `android:foreground="?attr/selectableItemBackgroundBorderless"` for ripple.

✅ 6. ADD ICONS
- Icons to add: user, lock, phone, email.
- Implementation:
  - Use vector drawables inside `TextInputLayout` start/end icons or `app:startIconDrawable`.
  - Prefer Material Icons or custom vectordrawables.

✅ 7. OTP SCREEN IMPROVEMENT
- Divide into sections:
  - SECTION 1: User details
  - SECTION 2: OTP verification
  - SECTION 3: Password setup
- UX rule:
  - Hide OTP + password fields until "Send OTP" clicked.
  - Show progress & success toast on send.
- Implementation:
  - Use `ViewStub` or `include` with `visibility=gone` initially; toggle on click.

✅ 8. ADD LOADING STATES
- When: login, register, send money
- Show: `ProgressBar`, disable button, and a success/failure state.
- Implementation:
  - Use indeterminate circular `ProgressBar` or inline progress in button (swap text with spinner).
  - Disable inputs & buttons until request completes.

✅ 9. BACK BUTTON CONSISTENCY
- Every screen should have a consistent top app bar:
  - Left: Back arrow
  - Center: Screen Title
- Implementation:
  - Create a single `TopAppBar` layout and reuse across activities/fragments.
  - Use `NavController` with `AppBarConfiguration` for consistent back behavior.

✅ 10. DASHBOARD SHOULD FEEL PREMIUM
- Add:
  - Wallet balance card (gradient)
  - Quick actions row (Send / Add Money / History)
  - Recent transactions list
- Wallet card idea:
  - Title: Available Balance
  - Big amount: ₹ 12,500
  - Actions inside card: Send | Add Money | History
- Implementation:
  - Use `CardView` or Material `Card` with gradient background, rounded corners, padding.
  - Quick actions: icon buttons with labels.

✅ 11. ADD BOTTOM NAVIGATION
- Tabs:
  - Home -> Dashboard
  - Transactions -> History
  - Profile -> Account
- Implementation:
  - Use `BottomNavigationView` and `Navigation Component` with 3 top-level destinations.

✅ 12. FONT
- Use modern fonts:
  - Poppins or Inter
- Implementation:
  - Add via `res/font` (download) and apply via theme `fontFamily`.
  - Use weights: Regular for body, SemiBold/Bold for headings and buttons.

LOGO IDEA
- Simple wallet icon:
  - Single-letter C or wallet silhouette
  - Gradient purple fill (`#6C4AB6` -> `#4F2D96`)

VERY IMPORTANT — DO NOT:
- Over-design, use too many colors, or heavy animations.
- Keep app: clean, trustworthy, smooth, minimal.

EXTRA IMPLEMENTATION SNIPPETS

- colors.xml (add)
```xml
<color name="purple_500">#6C4AB6</color>
<color name="purple_700">#4F2D96</color>
<color name="bg_soft">#FAFAFC</color>
<color name="text_primary">#1E1E1E</color>
<color name="hint_text">#9E9E9E</color>
```

- Example Material filled input (layout)
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.FilledBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:boxBackgroundMode="filled"
    app:boxCornerRadiusTopStart="14dp"
    app:boxCornerRadiusTopEnd="14dp"
    app:startIconDrawable="@drawable/ic_person">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:hint="Phone number" />
</com.google.android.material.textfield.TextInputLayout>
```

- Example button style (styles.xml)
```xml
<style name="Widget.App.PrimaryButton" parent="Widget.MaterialComponents.Button">
    <item name="cornerRadius">18dp</item>
    <item name="android:height">52dp</item>
    <item name="android:textStyle">bold</item>
    <item name="android:paddingLeft">24dp</item>
    <item name="android:paddingRight">24dp</item>
</style>
```

PRIORITY ORDER FOR TOMORROW
- FIRST: logo, toolbar, modern inputs, button redesign
- SECOND: dashboard card, icons, loading states

TASKS / IMPLEMENTATION STEPS (suggested)
1. Add colors & fonts to resources.
2. Create logo vector and app icon.
3. Update theme to MaterialComponents + `colorPrimary`.
4. Replace EditText with `TextInputLayout` in critical screens (login, transfer).
5. Create primary button style & gradient drawable.
6. Implement toolbar component and refactor activities/fragments to use it.
7. Build dashboard wallet card and bottom navigation.
8. Add icons and inline loading spinners.
9. Test flows (login, OTP, transfer) and tune paddings & elevation.

Notes:
- I can provide individual PR-ready diffs/snippets for each step. Tell me which item to implement first and I will prepare the code changes.


2. Make Login Section a Card

Currently fields are floating.

Put everything inside:

white card
rounded corners (20dp)
shadow/elevation

Like:

PhonePe
Paytm
CRED
Slice

Structure:

Logo
Welcome Text
Inputs
Login Button

inside one centered container.
