package de.propra2.ausleiherino24.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class PPTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "ppTransaction")
    Case aCase;

    Double lendingCost;

    boolean cautionPaid;

    Long reservationId;

    /**
     * TODO JavaDoc.
     *
     * @return Description
     */
    public Double getTotalPayment() {
        if (cautionPaid) {
            return lendingCost + aCase.getDeposit();
        }
        return lendingCost;
    }


}