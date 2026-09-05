-- ====================================================================
-- ReviewTask: Supabase & PostgreSQL Production Schema
-- Genuine Customer Feedback Platform with Non-Manipulated Review Policy
-- ====================================================================

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    phone TEXT NOT NULL UNIQUE,
    whatsapp TEXT,
    upi_id TEXT,
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN', 'BUSINESS')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    referral_code TEXT UNIQUE,
    referred_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Businesses Table
CREATE TABLE IF NOT EXISTS public.businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    business_name TEXT NOT NULL,
    category TEXT NOT NULL,
    address TEXT NOT NULL,
    google_maps_url TEXT NOT NULL,
    verification_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Campaigns Table
CREATE TABLE IF NOT EXISTS public.campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID REFERENCES public.businesses(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    reward_amount NUMERIC(10, 2) NOT NULL CHECK (reward_amount > 0),
    total_budget NUMERIC(10, 2) NOT NULL CHECK (total_budget >= reward_amount),
    remaining_budget NUMERIC(10, 2) NOT NULL CHECK (remaining_budget >= 0),
    max_participants INT NOT NULL DEFAULT 50,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'PAUSED', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Tasks Table (Submissions & Verification)
CREATE TABLE IF NOT EXISTS public.tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID REFERENCES public.campaigns(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('AVAILABLE', 'IN_PROGRESS', 'PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    reward_amount NUMERIC(10, 2) NOT NULL CHECK (reward_amount > 0),
    proof TEXT,
    admin_note TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_user_campaign UNIQUE (user_id, campaign_id)
);

-- 5. Feedback Table (Genuine Customer Feedback)
CREATE TABLE IF NOT EXISTS public.feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL UNIQUE REFERENCES public.tasks(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    answers JSONB NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. Wallets Table
CREATE TABLE IF NOT EXISTS public.wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES public.profiles(id) ON DELETE CASCADE,
    balance NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    total_earned NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_earned >= 0),
    total_withdrawn NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_withdrawn >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 7. Wallet Transactions Table
CREATE TABLE IF NOT EXISTS public.wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    reference_id TEXT,
    status TEXT NOT NULL DEFAULT 'SUCCESS' CHECK (status IN ('SUCCESS', 'PENDING', 'FAILED')),
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 8. Withdrawals Table (Manual Admin Processing)
CREATE TABLE IF NOT EXISTS public.withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    upi_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'PROCESSING', 'SUCCESSFUL', 'REJECTED')),
    admin_note TEXT,
    transaction_reference TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    admin_id UUID REFERENCES public.profiles(id)
);

-- 9. Referrals Table
CREATE TABLE IF NOT EXISTS public.referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    referred_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    bonus NUMERIC(10, 2) NOT NULL DEFAULT 10.00,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 10. Settings Table
CREATE TABLE IF NOT EXISTS public.settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Default Settings
INSERT INTO public.settings (key, value) VALUES
('min_withdrawal_amount', '50.00'),
('referral_bonus_amount', '10.00'),
('anti_fraud_cooldown_hours', '24'),
('platform_notice', 'ReviewTask strictly collects voluntary customer insights. Google Maps reviews are voluntary and never rewarded.')
ON CONFLICT (key) DO NOTHING;

-- ====================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.businesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallet_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.withdrawals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.settings ENABLE ROW LEVEL SECURITY;

-- Helper function to check if caller is an Admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'ADMIN' AND status = 'ACTIVE'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Profiles Policies
CREATE POLICY "Users can read their own profile or admins can view all"
ON public.profiles FOR SELECT
USING (auth.uid() = id OR public.is_admin());

CREATE POLICY "Users can update their own profile"
ON public.profiles FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id AND role = 'USER');

-- Businesses Policies
CREATE POLICY "Anyone can view approved businesses"
ON public.businesses FOR SELECT
USING (verification_status = 'APPROVED' OR owner_id = auth.uid() OR public.is_admin());

CREATE POLICY "Business owners can insert and update their businesses"
ON public.businesses FOR ALL
USING (owner_id = auth.uid() OR public.is_admin());

-- Campaigns Policies
CREATE POLICY "Anyone can read active campaigns"
ON public.campaigns FOR SELECT
USING (status = 'ACTIVE' OR public.is_admin());

-- Tasks Policies
CREATE POLICY "Users can view own tasks or admin can view all"
ON public.tasks FOR SELECT
USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "Users can insert own task submissions"
ON public.tasks FOR INSERT
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own in-progress or pending tasks"
ON public.tasks FOR UPDATE
USING (user_id = auth.uid() AND status IN ('AVAILABLE', 'IN_PROGRESS', 'PENDING'))
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Admins can update and review all tasks"
ON public.tasks FOR ALL
USING (public.is_admin());

-- Feedback Policies
CREATE POLICY "Users can insert feedback for their own tasks"
ON public.feedback FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.tasks
    WHERE tasks.id = task_id AND tasks.user_id = auth.uid()
  )
);

CREATE POLICY "Users can view their own feedback, businesses can view campaign feedback"
ON public.feedback FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM public.tasks
    WHERE tasks.id = feedback.task_id AND (tasks.user_id = auth.uid() OR public.is_admin())
  )
);

-- Wallets Policies: Users can ONLY read their own wallet, NEVER update balance directly
CREATE POLICY "Users can read own wallet"
ON public.wallets FOR SELECT
USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "Only admins or backend can update wallet"
ON public.wallets FOR UPDATE
USING (public.is_admin());

-- Wallet Transactions Policies
CREATE POLICY "Users can read own transactions"
ON public.wallet_transactions FOR SELECT
USING (user_id = auth.uid() OR public.is_admin());

-- Withdrawals Policies: Users can create and read own requests
CREATE POLICY "Users can read and submit own withdrawals"
ON public.withdrawals FOR SELECT
USING (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "Users can insert withdrawal requests"
ON public.withdrawals FOR INSERT
WITH CHECK (user_id = auth.uid());

CREATE POLICY "Only admins can update withdrawal status"
ON public.withdrawals FOR UPDATE
USING (public.is_admin());

-- Settings Policies: Everyone can read, only admin can update
CREATE POLICY "Anyone can read settings"
ON public.settings FOR SELECT
TO authenticated, anon
USING (true);

CREATE POLICY "Only admin can modify settings"
ON public.settings FOR ALL
USING (public.is_admin());
