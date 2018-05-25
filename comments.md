2018-03-24 20:29:30 AGB
## State Machine initial - wrong

### workflow inseguro
inicial -> completado [final]

### workflow seguro 
initial          -> completed
                 -> denied

initial -LAMBDA-> waiting outgoing confirmation State
waiting outgoing confirmation State -confirmed-> incoming request State
                                    -denied-> deniedState [final]

incoming request State -LAMBDA-> waiting incoming request State

waiting incoming confirmation State -confirmed-> completed State [final]
                                    -denied-> deniedState [final]

2018-03-24 20:29:44 AGB
## State machine 2

### workflow all together
initial -outgoing enabled?-> waiting outgoing confirmation State
        -outgoing disable?-> incoming request State

waiting outgoing confirmation State -confirmed-> incoming request State
                                    -denied-> deniedState [final]

incoming request State -incoming enabled?-> waiting incoming confirmation State
                       -incoming disable?-> confirmed

waiting incoming confirmation State -confirmed-> completed State [final]
                                    -denied-> deniedState [final]
