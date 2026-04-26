package io.vainslab.onemoresubscriber.handler;

import io.vainslab.onemoresubscriber.operation.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultServiceHandler extends AbstractServiceHandler {

    public DefaultServiceHandler(ApplyOperation apply, JoinOperation join, LeaveOperation leave,
                                  MakePaymentOperation pay, MyPaymentsOperation myPay,
                                  DeletePaymentOperation delPay, TipOperation tip,
                                  MessageCreatorOperation msg, BackOperation back) {
        super(List.of(apply, join, leave, pay, myPay, delPay, tip, msg, back));
    }

    @Override
    public String getServiceType() {
        return "DEFAULT";
    }
}
