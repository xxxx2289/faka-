<?php
declare(strict_types=1);

namespace App\Controller\User\Api;

use App\Controller\Base\API\User;
use App\Interceptor\UserSession;
use App\Interceptor\Waf;
use App\Util\Date;
use Kernel\Annotation\Interceptor;
use Kernel\Exception\JSONException;

/**
 * 移动端用户看板接口（只读）。
 * 返回个人资产、消费统计、等级信息、经营数据（商家），供 App 首页/我的页展示。
 * 数据口径与网页端 Dashboard 一致。
 */
#[Interceptor([Waf::class, UserSession::class], Interceptor::TYPE_API)]
class Dashboard extends User
{
    /**
     * 汇总看板数据
     * @return array
     * @throws JSONException
     */
    public function index(): array
    {
        $user = $this->getUser();
        if (!$user) {
            throw new JSONException("请先登录");
        }
        $uid = (int)$user->id;
        $monthStart = date("Y-m-01 00:00:00");

        // ---------- 资产（所有人）----------
        $asset = [
            'balance'   => (float)$user->balance,      // 余额
            'coin'      => (float)$user->coin,          // 硬币
            'integral'  => (int)$user->integral,        // 积分
            'recharge'  => (float)$user->recharge,      // 元气（累计充值）
            'total_coin'=> (float)$user->total_coin,    // 总硬币收入
        ];

        // ---------- 消费统计（所有人）----------
        $buyModel = \App\Model\Order::query()->where("owner", $uid)->where("status", 1);
        $consume = [
            'buy_month' => (float)sprintf("%.2f", (float)(clone $buyModel)->where("create_time", ">=", $monthStart)->sum("amount")),
            'buy_total' => (float)sprintf("%.2f", (float)(clone $buyModel)->sum("amount")),
            'buy_count' => (int)(clone $buyModel)->count(),
        ];

        // ---------- 会员等级 ----------
        $group = \App\Model\UserGroup::get((float)$user->recharge);
        $nextGroup = \App\Model\UserGroup::get((float)$user->recharge, true);
        $level = [
            'name'          => $group ? (string)$group->name : '',
            'recharge'      => $group ? (float)$group->recharge : 0,   // 当前等级门槛
            'next_name'     => $nextGroup ? (string)$nextGroup->name : '',
            'next_recharge' => $nextGroup ? (float)$nextGroup->recharge : 0, // 下一级门槛（0 表示顶级）
        ];

        $data = [
            'username' => (string)$user->username,
            'avatar'   => (string)$user->avatar,
            'asset'    => $asset,
            'consume'  => $consume,
            'level'    => $level,
            'is_business' => (int)$user->business_level > 0,
        ];

        // ---------- 经营数据（商家）----------
        if ((int)$user->business_level > 0) {
            $bill = \App\Model\Bill::query()->where("owner", $uid)->where("type", 1)->where("currency", 1);
            $sellModel = \App\Model\Order::query()->where("user_id", $uid)->where("status", 1);

            $business = [
                'today_income'     => (float)sprintf("%.2f", (float)(clone $bill)->whereBetween('create_time', [Date::calcDay(), Date::calcDay(0, Date::TYPE_END)])->sum("amount")),
                'yesterday_income' => (float)sprintf("%.2f", (float)(clone $bill)->whereBetween('create_time', [Date::calcDay(-1), Date::calcDay(-1, Date::TYPE_END)])->sum("amount")),
                'week_income'      => (float)sprintf("%.2f", (float)(clone $bill)->whereBetween('create_time', [Date::weekDay(1, Date::TYPE_START), Date::weekDay(7, Date::TYPE_END)])->sum("amount")),
                'month_income'     => (float)sprintf("%.2f", (float)(clone $bill)->where("create_time", ">=", $monthStart)->sum("amount")),
                'trade_total'      => (float)sprintf("%.2f", (float)(clone $sellModel)->sum("amount")),
                'today_orders'     => (int)(clone $sellModel)->whereBetween('create_time', [Date::calcDay(), Date::calcDay(0, Date::TYPE_END)])->count(),
                'pending_delivery' => (int)(clone $sellModel)->where("delivery_status", 0)->count(),
                'card_unsold'      => (int)\App\Model\Card::query()->where("owner", $uid)->where("status", 0)->count(),
                'commodity_online' => (int)\App\Model\Commodity::query()->where("owner", $uid)->where("status", 1)->count(),
                'commodity_count'  => (int)\App\Model\Commodity::query()->where("owner", $uid)->count(),
            ];
            $data['business'] = $business;
        } else {
            $data['business'] = null;
        }

        return $this->json(200, "success", $data);
    }
}
