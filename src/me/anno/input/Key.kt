package me.anno.input

import me.anno.utils.structures.lists.Lists.arrayListOfNulls

/**
 * List of all keycodes, baseline is GLFW (and ASCII), because that's our default platform;
 * */
enum class Key(val id: Int) {

    /**
     * Unknown/undefined key
     * */
    KEY_UNKNOWN(-1),

    /**
     * Mouse buttons
     * */
    BUTTON_LEFT(0),
    BUTTON_RIGHT(1),
    BUTTON_MIDDLE(2),
    BUTTON_BACK(3),
    BUTTON_FORWARD(4),
    BUTTON_6(5),
    BUTTON_7(6),
    BUTTON_8(7),

    KEY_SPACE(32),
    KEY_APOSTROPHE(39),
    KEY_COMMA(44),
    KEY_MINUS(45),
    KEY_PERIOD(46),
    KEY_SLASH(47),

    /**
     * Digits
     * */
    KEY_0(48),
    KEY_1(49),
    KEY_2(50),
    KEY_3(51),
    KEY_4(52),
    KEY_5(53),
    KEY_6(54),
    KEY_7(55),
    KEY_8(56),
    KEY_9(57),

    KEY_SEMICOLON(59),
    KEY_EQUAL(61),

    /**
     * Letters of the alphabet
     * */
    KEY_A(65),
    KEY_B(66),
    KEY_C(67),
    KEY_D(68),
    KEY_E(69),
    KEY_F(70),
    KEY_G(71),
    KEY_H(72),
    KEY_I(73),
    KEY_J(74),
    KEY_K(75),
    KEY_L(76),
    KEY_M(77),
    KEY_N(78),
    KEY_O(79),
    KEY_P(80),
    KEY_Q(81),
    KEY_R(82),
    KEY_S(83),
    KEY_T(84),
    KEY_U(85),
    KEY_V(86),
    KEY_W(87),
    KEY_X(88),
    KEY_Y(89),
    KEY_Z(90),

    KEY_LEFT_BRACKET(91),
    KEY_BACKSLASH(92),
    KEY_RIGHT_BRACKET(93),
    KEY_GRAVE_ACCENT(96),
    KEY_WORLD_1(161),
    KEY_WORLD_2(162),
    KEY_ESCAPE(256),
    KEY_ENTER(257),
    KEY_TAB(258),
    KEY_BACKSPACE(259),
    KEY_INSERT(260),
    KEY_DELETE(261),
    KEY_ARROW_RIGHT(262),
    KEY_ARROW_LEFT(263),
    KEY_ARROW_DOWN(264),
    KEY_ARROW_UP(265),
    KEY_PAGE_UP(266),
    KEY_PAGE_DOWN(267),
    KEY_HOME(268),
    KEY_END(269),
    KEY_CAPS_LOCK(280),
    KEY_SCROLL_LOCK(281),
    KEY_NUM_LOCK(282),
    KEY_PRINT_SCREEN(283),
    KEY_PAUSE(284),

    // function keys
    KEY_F1(290),
    KEY_F2(291),
    KEY_F3(292),
    KEY_F4(293),
    KEY_F5(294),
    KEY_F6(295),
    KEY_F7(296),
    KEY_F8(297),
    KEY_F9(298),
    KEY_F10(299),
    KEY_F11(300),
    KEY_F12(301),
    KEY_F13(302),
    KEY_F14(303),
    KEY_F15(304),
    KEY_F16(305),
    KEY_F17(306),
    KEY_F18(307),
    KEY_F19(308),
    KEY_F20(309),
    KEY_F21(310),
    KEY_F22(311),
    KEY_F23(312),
    KEY_F24(313),
    KEY_F25(314),

    // kp = keypad/numpad
    KEY_KP_0(320),
    KEY_KP_1(321),
    KEY_KP_2(322),
    KEY_KP_3(323),
    KEY_KP_4(324),
    KEY_KP_5(325),
    KEY_KP_6(326),
    KEY_KP_7(327),
    KEY_KP_8(328),
    KEY_KP_9(329),
    KEY_KP_DECIMAL(330),
    KEY_KP_DIVIDE(331),
    KEY_KP_MULTIPLY(332),
    KEY_KP_SUBTRACT(333),
    KEY_KP_ADD(334),
    KEY_KP_ENTER(335),
    KEY_KP_EQUAL(336),

    // super/special keys
    KEY_LEFT_SHIFT(340),
    KEY_LEFT_CONTROL(341),
    KEY_LEFT_ALT(342),
    KEY_LEFT_SUPER(343),
    KEY_RIGHT_SHIFT(344),
    KEY_RIGHT_CONTROL(345),
    KEY_RIGHT_ALT(346),
    KEY_RIGHT_SUPER(347),
    KEY_MENU(348),

    // these probably should be handled differently...
    // if you need more buttons, check Input.controllers[i].buttonDownTime/axisValues GLFW directly
    /**
     * Controller keys
     * */
    CONTROLLER_0_KEY_0(400),
    CONTROLLER_0_KEY_1(401),
    CONTROLLER_0_KEY_2(402),
    CONTROLLER_0_KEY_3(403),
    CONTROLLER_0_KEY_4(404),
    CONTROLLER_0_KEY_5(405),
    CONTROLLER_0_KEY_6(406),
    CONTROLLER_0_KEY_7(407),
    CONTROLLER_0_KEY_8(408),
    CONTROLLER_0_KEY_9(409),
    CONTROLLER_0_KEY_10(410),
    CONTROLLER_0_KEY_11(411),
    CONTROLLER_0_KEY_12(412),
    CONTROLLER_0_KEY_13(413),
    CONTROLLER_0_KEY_14(414),
    CONTROLLER_0_KEY_15(415),
    CONTROLLER_0_KEY_16(416),
    CONTROLLER_0_KEY_17(417),
    CONTROLLER_0_KEY_18(418),
    CONTROLLER_0_KEY_19(419),
    CONTROLLER_0_KEY_20(420),
    CONTROLLER_0_KEY_21(421),
    CONTROLLER_0_KEY_22(422),
    CONTROLLER_0_KEY_23(423),
    CONTROLLER_0_KEY_24(424),
    CONTROLLER_0_KEY_25(425),
    CONTROLLER_0_KEY_26(426),
    CONTROLLER_0_KEY_27(427),
    CONTROLLER_0_KEY_28(428),
    CONTROLLER_0_KEY_29(429),
    CONTROLLER_0_KEY_30(430),
    CONTROLLER_0_KEY_31(431),

    CONTROLLER_1_KEY_0(432),
    CONTROLLER_1_KEY_1(433),
    CONTROLLER_1_KEY_2(434),
    CONTROLLER_1_KEY_3(435),
    CONTROLLER_1_KEY_4(436),
    CONTROLLER_1_KEY_5(437),
    CONTROLLER_1_KEY_6(438),
    CONTROLLER_1_KEY_7(439),
    CONTROLLER_1_KEY_8(440),
    CONTROLLER_1_KEY_9(441),
    CONTROLLER_1_KEY_10(442),
    CONTROLLER_1_KEY_11(443),
    CONTROLLER_1_KEY_12(444),
    CONTROLLER_1_KEY_13(445),
    CONTROLLER_1_KEY_14(446),
    CONTROLLER_1_KEY_15(447),
    CONTROLLER_1_KEY_16(448),
    CONTROLLER_1_KEY_17(449),
    CONTROLLER_1_KEY_18(450),
    CONTROLLER_1_KEY_19(451),
    CONTROLLER_1_KEY_20(452),
    CONTROLLER_1_KEY_21(453),
    CONTROLLER_1_KEY_22(454),
    CONTROLLER_1_KEY_23(455),
    CONTROLLER_1_KEY_24(456),
    CONTROLLER_1_KEY_25(457),
    CONTROLLER_1_KEY_26(458),
    CONTROLLER_1_KEY_27(459),
    CONTROLLER_1_KEY_28(460),
    CONTROLLER_1_KEY_29(461),
    CONTROLLER_1_KEY_30(462),
    CONTROLLER_1_KEY_31(463),

    CONTROLLER_2_KEY_0(464),
    CONTROLLER_2_KEY_1(465),
    CONTROLLER_2_KEY_2(466),
    CONTROLLER_2_KEY_3(467),
    CONTROLLER_2_KEY_4(468),
    CONTROLLER_2_KEY_5(469),
    CONTROLLER_2_KEY_6(470),
    CONTROLLER_2_KEY_7(471),
    CONTROLLER_2_KEY_8(472),
    CONTROLLER_2_KEY_9(473),
    CONTROLLER_2_KEY_10(474),
    CONTROLLER_2_KEY_11(475),
    CONTROLLER_2_KEY_12(476),
    CONTROLLER_2_KEY_13(477),
    CONTROLLER_2_KEY_14(478),
    CONTROLLER_2_KEY_15(479),
    CONTROLLER_2_KEY_16(480),
    CONTROLLER_2_KEY_17(481),
    CONTROLLER_2_KEY_18(482),
    CONTROLLER_2_KEY_19(483),
    CONTROLLER_2_KEY_20(484),
    CONTROLLER_2_KEY_21(485),
    CONTROLLER_2_KEY_22(486),
    CONTROLLER_2_KEY_23(487),
    CONTROLLER_2_KEY_24(488),
    CONTROLLER_2_KEY_25(489),
    CONTROLLER_2_KEY_26(490),
    CONTROLLER_2_KEY_27(491),
    CONTROLLER_2_KEY_28(492),
    CONTROLLER_2_KEY_29(493),
    CONTROLLER_2_KEY_30(494),
    CONTROLLER_2_KEY_31(495),

    CONTROLLER_3_KEY_0(496),
    CONTROLLER_3_KEY_1(497),
    CONTROLLER_3_KEY_2(498),
    CONTROLLER_3_KEY_3(499),
    CONTROLLER_3_KEY_4(500),
    CONTROLLER_3_KEY_5(501),
    CONTROLLER_3_KEY_6(502),
    CONTROLLER_3_KEY_7(503),
    CONTROLLER_3_KEY_8(504),
    CONTROLLER_3_KEY_9(505),
    CONTROLLER_3_KEY_10(506),
    CONTROLLER_3_KEY_11(507),
    CONTROLLER_3_KEY_12(508),
    CONTROLLER_3_KEY_13(509),
    CONTROLLER_3_KEY_14(510),
    CONTROLLER_3_KEY_15(511),
    CONTROLLER_3_KEY_16(512),
    CONTROLLER_3_KEY_17(513),
    CONTROLLER_3_KEY_18(514),
    CONTROLLER_3_KEY_19(515),
    CONTROLLER_3_KEY_20(516),
    CONTROLLER_3_KEY_21(517),
    CONTROLLER_3_KEY_22(518),
    CONTROLLER_3_KEY_23(519),
    CONTROLLER_3_KEY_24(520),
    CONTROLLER_3_KEY_25(521),
    CONTROLLER_3_KEY_26(522),
    CONTROLLER_3_KEY_27(523),
    CONTROLLER_3_KEY_28(524),
    CONTROLLER_3_KEY_29(525),
    CONTROLLER_3_KEY_30(526),
    CONTROLLER_3_KEY_31(527),
    // this list may be extended in the future...
    ;

    fun isClickKey(buttonsToo: Boolean) = when (this) {
        BUTTON_LEFT, BUTTON_MIDDLE, BUTTON_RIGHT -> buttonsToo
        KEY_ENTER, KEY_KP_ENTER -> true
        else -> false
    }

    fun isDownKey() = when (this) {
        KEY_ENTER, KEY_KP_ENTER, KEY_ARROW_DOWN -> true
        else -> false
    }

    fun isUpKey() = when (this) {
        KEY_ARROW_UP -> true
        else -> false
    }

    companion object {
        private val byId = arrayListOfNulls<Key>(528)

        init {
            for (v in entries) {
                if (v.id >= 0) {
                    byId[v.id] = v
                }
            }
        }

        fun byId(i: Int): Key {
            return byId.getOrNull(i) ?: KEY_UNKNOWN
        }
    }
}