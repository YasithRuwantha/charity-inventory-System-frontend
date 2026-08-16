import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { Mail, Lock, User } from "lucide-react";
import { AuthLayout } from "@/features/auth/AuthLayout";
import { Input, Label, FieldError, Select } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { register as registerUser } from "@/api/auth";
import { useAuthStore } from "@/store/auth";
import { ApiRequestError } from "@/api/types";
import { notifySuccess } from "@/lib/toast";

const schema = z.object({
  name: z.string().min(2, "Enter your full name"),
  email: z.string().min(1, "Email is required").email("Enter a valid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  role: z.enum(["ADMIN", "INVENTORY_STAFF", "VOLUNTEER"]),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { role: "INVENTORY_STAFF" } });

  const onSubmit = async (values: FormValues) => {
    setFormError(null);
    try {
      const res = await registerUser(values);
      setSession(res.token, { name: res.name, email: res.email, role: res.role });
      notifySuccess(`Welcome, ${res.name.split(" ")[0]} — your account is ready.`);
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiRequestError) {
        setFormError(err.message);
      } else {
        setFormError("Unable to create your account. Please try again.");
      }
    }
  };

  return (
    <AuthLayout title="Create your account" subtitle="Register to start managing donations and inventory.">
      {formError && (
        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-sm text-rose-700">
          {formError}
        </div>
      )}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <div>
          <Label htmlFor="name">Full name</Label>
          <Input
            id="name"
            autoComplete="name"
            placeholder="Jane Doe"
            leadingIcon={<User className="h-4 w-4" />}
            invalid={!!errors.name}
            {...register("name")}
          />
          <FieldError>{errors.name?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="email">Email address</Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@organization.org"
            leadingIcon={<Mail className="h-4 w-4" />}
            invalid={!!errors.email}
            {...register("email")}
          />
          <FieldError>{errors.email?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="At least 8 characters"
            leadingIcon={<Lock className="h-4 w-4" />}
            invalid={!!errors.password}
            {...register("password")}
          />
          <FieldError>{errors.password?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="role">Role</Label>
          <Controller
            control={control}
            name="role"
            render={({ field }) => (
              <Select id="role" {...field}>
                <option value="INVENTORY_STAFF">Inventory Staff</option>
                <option value="VOLUNTEER">Volunteer</option>
                <option value="ADMIN">Administrator</option>
              </Select>
            )}
          />
          <FieldError>{errors.role?.message}</FieldError>
        </div>
        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
          Create account
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-ink-500">
        Already have an account?{" "}
        <Link to="/login" className="font-medium text-brand-700 hover:text-brand-800">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
