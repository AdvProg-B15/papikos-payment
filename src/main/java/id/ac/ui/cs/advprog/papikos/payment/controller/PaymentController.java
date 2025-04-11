package id.ac.ui.cs.advprog.papikos.payment.controller;

import id.ac.ui.cs.advprog.papikos.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/topup/{userId}")
    public ResponseEntity<?> topUp(@PathVariable String userId, @RequestParam long amount) {
        paymentService.topUp(userId, amount);
        return ResponseEntity.ok().build();
    }
}
