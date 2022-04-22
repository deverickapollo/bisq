/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.paymentmethods;

import bisq.desktop.components.InputTextField;
import bisq.desktop.util.FormBuilder;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.locale.CountryUtil;
import bisq.core.locale.Res;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.UpiAccount;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.UpiAccountPayload;
import bisq.core.util.coin.CoinFormatter;
import bisq.core.util.validation.InputValidator;

import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextField;
import static bisq.desktop.util.FormBuilder.addCompactTopLabelTextFieldWithCopyIcon;
import static bisq.desktop.util.FormBuilder.addTopLabelTextField;

public class UpiForm extends PaymentMethodForm {
    private final UpiAccount account;

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        addCompactTopLabelTextFieldWithCopyIcon(gridPane, ++gridRow, Res.get("payment.upi.virtualPaymentAddress"),
                ((UpiAccountPayload) paymentAccountPayload).getVirtualPaymentAddress());
        return gridRow;
    }

    public UpiForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService,
                       InputValidator inputValidator, GridPane gridPane,
                       int gridRow, CoinFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.account = (UpiAccount) paymentAccount;
    }

    @Override
    public void addFormForAddAccount() {
        // this payment method is only for India/INR
        account.setSingleTradeCurrency(account.getSupportedCurrencies().get(0));
        CountryUtil.findCountryByCode("IN").ifPresent(c -> account.setCountry(c));

        gridRowFrom = gridRow + 1;

        InputTextField virtualPaymentAddressInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, Res.get("payment.upi.virtualPaymentAddress"));
        virtualPaymentAddressInputTextField.setValidator(inputValidator);
        virtualPaymentAddressInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            account.setVirtualPaymentAddress(newValue.trim());
            updateFromInputs();
        });

        addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), account.getSingleTradeCurrency().getNameAndCode());
        addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.country"), account.getCountry().name);
        addLimitations(false);
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(account.getVirtualPaymentAddress());
    }

    @Override
    public void addFormForEditAccount() {
        gridRowFrom = gridRow;
        addAccountNameTextFieldWithAutoFillToggleButton();
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(account.getPaymentMethod().getId()));
        TextField field = addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("payment.upi.virtualPaymentAddress"),
                account.getVirtualPaymentAddress()).second;
        field.setMouseTransparent(false);
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), account.getSingleTradeCurrency().getNameAndCode());
        addCompactTopLabelTextField(gridPane, ++gridRow, Res.get("shared.country"), account.getCountry().name);
        addLimitations(true);
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && inputValidator.validate(account.getVirtualPaymentAddress()).isValid);
    }
}
