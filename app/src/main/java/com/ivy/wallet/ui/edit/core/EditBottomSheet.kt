package com.ivy.wallet.ui.edit.core

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.statusBarsPadding
import com.ivy.wallet.R
import com.ivy.wallet.base.*
import com.ivy.wallet.model.TransactionType
import com.ivy.wallet.model.entity.Account
import com.ivy.wallet.ui.IvyAppPreview
import com.ivy.wallet.ui.LocalIvyContext
import com.ivy.wallet.ui.theme.*
import com.ivy.wallet.ui.theme.components.*
import com.ivy.wallet.ui.theme.modal.DURATION_MODAL_KEYBOARD
import com.ivy.wallet.ui.theme.modal.ModalSave
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

@Composable
fun BoxWithConstraintsScope.EditBottomSheet(
    initialTransactionId: UUID?,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    amount: Double,
    currency: String,

    amountModalShown: Boolean,
    setAmountModalShown: (Boolean) -> Unit,
    ActionButton: @Composable () -> Unit,

    onAmountChanged: (Double) -> Unit,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit
) {
    val ivyContext = LocalIvyContext.current
    val rootView = LocalView.current
    var keyboardShown by remember { mutableStateOf(false) }

    onScreenStart {
        rootView.addKeyboardListener {
            keyboardShown = it
        }
    }

    val keyboardShownInsetDp by animateDpAsState(
        targetValue = densityScope {
            if (keyboardShown) keyboardOnlyWindowInsets().bottom.toDp() else 0.dp
        },
        animationSpec = tween(DURATION_MODAL_KEYBOARD)
    )
    val navBarPadding by animateDpAsState(
        targetValue = densityScope {
            if (keyboardShown) 0.dp else navigationBarInsets().bottom.toDp()
        },
        animationSpec = tween(DURATION_MODAL_KEYBOARD)
    )

    var bottomBarHeight by remember { mutableStateOf(0) }

    var internalExpanded by remember { mutableStateOf(true) }
    val expanded = internalExpanded && !keyboardShown

    val percentExpanded by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounce()
    )
    val percentCollapsed = 1f - percentExpanded

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
            .drawColoredShadow(
                color = IvyTheme.colors.mediumInverse,
                alpha = if (IvyTheme.colors.isLight) 0.3f else 0.2f,
                borderRadius = 24.dp,
                shadowRadius = 24.dp
            )
            .background(IvyTheme.colors.pure, Shapes.rounded24Top)
            .consumeClicks()
    ) {
        //Accounts label
        val label = when (type) {
            TransactionType.INCOME -> "Add money to"
            TransactionType.EXPENSE -> "Pay with"
            TransactionType.TRANSFER -> "From"
        }

        SheetHeader(
            percentExpanded = percentExpanded,
            label = label,
            type = type,
            accounts = accounts,
            selectedAccount = selectedAccount,
            toAccount = toAccount,
            onSelectedAccountChanged = onSelectedAccountChanged,
            onToAccountChanged = onToAccountChanged,
            onAddNewAccount = onAddNewAccount
        )

        val spacerAboveAmount = lerp(40, 16, percentCollapsed)
        Spacer(Modifier.height(spacerAboveAmount.dp))

        if (type == TransactionType.TRANSFER && percentExpanded < 1f) {
            TransferRowMini(
                percentCollapsed = percentCollapsed,
                fromAccount = selectedAccount,
                toAccount = toAccount,
                onSetExpanded = {
                    internalExpanded = true
                }
            )
        }

        Amount(
            type = type,
            amount = amount,
            currency = currency,
            label = label,
            account = selectedAccount,
            percentExpanded = percentExpanded,
            onShowAmountModal = {
                setAmountModalShown(true)
            },
            onAccountMiniClick = {
                hideKeyboard(rootView)
                internalExpanded = true
            }
        )

        val lastSpacer = lerp(20f, 8f, percentCollapsed)
        if (lastSpacer > 0) {
            Spacer(Modifier.height(lastSpacer.dp))
        }
//
        //system stuff + keyboard padding
        Spacer(Modifier.height(densityScope { bottomBarHeight.toDp() }))
        Spacer(Modifier.height(keyboardShownInsetDp))
    }

    BottomBar(
        keyboardShown = keyboardShown,
        expanded = expanded,
        internalExpanded = internalExpanded,
        setInternalExpanded = {
            internalExpanded = it
        },
        setBottomBarHeight = {
            bottomBarHeight = it
        },

        keyboardShownInsetDp = keyboardShownInsetDp,
        navBarPadding = navBarPadding,

        ActionButton = ActionButton
    )

    val amountModalId = remember(initialTransactionId, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalShown,
        currency = currency,
        initialAmount = amount.takeIf { it > 0 },
        Header = {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = "Account",
                style = Typo.body1.style(
                    color = IvyTheme.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "amount_modal_account"
            )
        },
        amountSpacerTop = 48.dp,
        dismiss = {
            setAmountModalShown(false)
        }
    ) {
        onAmountChanged(it)
    }
}

@Composable
private fun BottomBar(
    keyboardShown: Boolean,
    keyboardShownInsetDp: Dp,
    setBottomBarHeight: (Int) -> Unit,
    expanded: Boolean,
    internalExpanded: Boolean,
    setInternalExpanded: (Boolean) -> Unit,
    navBarPadding: Dp,
    ActionButton: @Composable () -> Unit
) {
    val ivyContext = LocalIvyContext.current

    ActionsRow(
        modifier = Modifier
            .onSizeChanged {
                setBottomBarHeight(it.height)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val systemOffsetBottom = keyboardShownInsetDp.toPx()
                val visibleHeight = placeable.height * 1f
                val y = ivyContext.screenHeight - visibleHeight - systemOffsetBottom

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        0,
                        y.roundToInt()
                    )
                }
            }
//            .gradientCutBackground()
            .padding(bottom = 12.dp)
            .padding(bottom = navBarPadding),
        lineColor = IvyTheme.colors.medium
    ) {
        Spacer(Modifier.width(24.dp))

        val expandRotation by animateFloatAsState(
            targetValue = if (expanded) 0f else -180f,
            animationSpec = springBounce()
        )

        val rootView = LocalView.current
        CircleButton(
            modifier = Modifier.rotate(expandRotation),
            icon = R.drawable.ic_expand_more,
        ) {
            setInternalExpanded(!internalExpanded || keyboardShown)
            hideKeyboard(rootView)
        }

        Spacer(Modifier.weight(1f))

        ActionButton()

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun TransferRowMini(
    percentCollapsed: Float,
    fromAccount: Account?,
    toAccount: Account?,
    onSetExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val height = placeable.height * (percentCollapsed)

                layout(placeable.width, height.roundToInt()) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(percentCollapsed)
            .clickableNoIndication {
                onSetExpanded()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val fromColor = fromAccount?.color?.toComposeColor() ?: Ivy
        val fromContrastColor = findContrastTextColor(fromColor)
        IvyButton(
            text = fromAccount?.name ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundGradient = Gradient.solid(fromColor),
            iconTint = fromContrastColor,
            textStyle = Typo.body2.style(
                color = fromContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }

        IvyIcon(
            icon = R.drawable.ic_arrow_right,
            tint = IvyTheme.colors.pureInverse
        )

        val toColor = toAccount?.color?.toComposeColor() ?: Ivy
        val toContrastColor = findContrastTextColor(toColor)
        IvyButton(
            text = toAccount?.name ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundGradient = Gradient.solid(toColor),
            iconTint = toContrastColor,
            textStyle = Typo.body2.style(
                color = toContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }
    }

    val transferMiniBottomSpacer = 20 * percentCollapsed
    if (transferMiniBottomSpacer > 0f) {
        Spacer(modifier = Modifier.height(transferMiniBottomSpacer.dp))
    }
}

@Composable
private fun SheetHeader(
    percentExpanded: Float,
    label: String,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
) {
    if (percentExpanded > 0.01f) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

//                    val x = lerp(0, ivyContext.screenWidth, (1f - percentExpanded))
                    val height = placeable.height * percentExpanded

                    layout(placeable.width, height.roundToInt()) {
                        placeable.placeRelative(
                            x = 0,
                            y = -(height * (1f - percentExpanded)).roundToInt(),
                        )
                    }
                }
                .alpha(percentExpanded)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = label,
                style = Typo.body1.style(
                    color = IvyTheme.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(if (type == TransactionType.TRANSFER) 8.dp else 16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "from_account"
            )

            if (type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(24.dp))

                Text(
                    modifier = Modifier.padding(start = 32.dp),
                    text = "To",
                    style = Typo.body1.style(
                        color = IvyTheme.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(8.dp))

                AccountsRow(
                    accounts = accounts,
                    selectedAccount = toAccount,
                    onSelectedAccountChanged = onToAccountChanged,
                    onAddNewAccount = onAddNewAccount,
                    childrenTestTag = "to_account",
                )
            }
        }
    }
}

@Composable
private fun AccountsRow(
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    selectedAccount: Account?,
    childrenTestTag: String? = null,
    onSelectedAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit
) {
    val lazyState = rememberLazyListState()

    LaunchedEffect(accounts, selectedAccount) {
        if (selectedAccount != null) {
            val selectedIndex = accounts.indexOf(selectedAccount)
            if (selectedIndex != -1) {
                launch {
                    if (TestingContext.inTest) return@launch //breaks UI tests

                    lazyState.scrollToItem(
                        index = selectedIndex, //+1 because Spacer width 24.dp
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = lazyState
    ) {
        item {
            Spacer(Modifier.width(24.dp))
        }

        itemsIndexed(accounts) { _, account ->
            Account(
                account = account,
                selected = selectedAccount == account,
                testTag = childrenTestTag ?: "account"
            ) {
                onSelectedAccountChanged(account)
            }
        }

        item {
            AddAccount {
                onAddNewAccount()
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun Account(
    account: Account,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val accountColor = account.color.toComposeColor()
    val textColor =
        if (selected) findContrastTextColor(accountColor) else IvyTheme.colors.pureInverse

    Row(
        modifier = Modifier
            .clip(Shapes.roundedFull)
            .thenIf(!selected) {
                border(2.dp, IvyTheme.colors.medium, Shapes.roundedFull)
            }
            .thenIf(selected) {
                background(accountColor, Shapes.roundedFull)
            }
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            iconName = account.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = textColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = account.name,
            style = Typo.body2.style(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }

    Spacer(Modifier.width(8.dp))
}

@Composable
private fun AddAccount(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(Shapes.roundedFull)
            .border(2.dp, IvyTheme.colors.medium, Shapes.roundedFull)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(
            icon = R.drawable.ic_plus,
            tint = IvyTheme.colors.pureInverse
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = "Add account",
            style = Typo.body2.style(
                color = IvyTheme.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }

    Spacer(Modifier.width(8.dp))
}

@Composable
private fun Amount(
    type: TransactionType,
    amount: Double,
    currency: String,
    percentExpanded: Float,
    label: String,
    account: Account?,
    onShowAmountModal: () -> Unit,
    onAccountMiniClick: () -> Unit,
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val percentCollapsed = 1f - percentExpanded
        val integerFontSize = lerp(40, 30, percentCollapsed)
        val spacerInteger = lerp(4, 0, percentCollapsed)
        val currencyPaddingTop = lerp(8, 4, percentCollapsed)
        val currencyFontSize = lerp(30, 18, percentCollapsed)

        Spacer(Modifier.width(32.dp))

        if (percentExpanded > 0.01f) {
            Spacer(
                Modifier.weight(
                    (1f * percentExpanded).coerceAtLeast(0.01f)
                )
            )
        }

        BalanceRow(
            modifier = Modifier
                .clickableNoIndication {
                    onShowAmountModal()
                }
                .testTag("edit_amount_balance_row"),
            currency = currency,
            balance = amount,

            decimalPaddingTop = currencyPaddingTop.dp,
            spacerDecimal = spacerInteger.dp,
            spacerCurrency = 8.dp,


            integerFontSize = integerFontSize.sp,
            decimalFontSize = 18.sp,
            currencyFontSize = currencyFontSize.sp,

            currencyUpfront = false
        )

        Spacer(Modifier.weight(1f))

        if (percentExpanded < 1f && type != TransactionType.TRANSFER) {
            LabelAccountMini(
                percentExpanded = percentExpanded,
                label = label,
                account = account,
                onClick = onAccountMiniClick
            )
        }

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun LabelAccountMini(
    percentExpanded: Float,
    label: String,
    account: Account?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val width = placeable.width * (1f - percentExpanded)

                layout(width.roundToInt(), placeable.height) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(1f - percentExpanded)
            .clickableNoIndication(
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = Typo.numberCaption.style(
                color = IvyTheme.colors.mediumInverse,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = account?.name?.toUpperCase(Locale.getDefault()) ?: "",
            style = Typo.numberBody2.style(
                color = IvyTheme.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    IvyAppPreview {
        val acc1 = Account("Cash", color = Green.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = null,
                type = TransactionType.INCOME,
                ActionButton = {
                    ModalSet() {

                    }
                },
                accounts = listOf(
                    acc1,
                    Account("DSK", color = GreenDark.toArgb()),
                    Account("phyre", color = GreenLight.toArgb()),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = null,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Transfer() {
    IvyAppPreview {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = UUID.randomUUID(),
                ActionButton = {
                    ModalSave {

                    }
                },
                type = TransactionType.TRANSFER,
                accounts = listOf(
                    acc1,
                    acc2,
                    Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = acc2,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}