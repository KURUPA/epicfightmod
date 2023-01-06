package yesman.epicfight.api.utils;

public class AttackResult {
	public final ResultType resultType;
	public final float damage;
	
	public AttackResult(ResultType resultType, float damage) {
		this.resultType = resultType;
		this.damage = damage;
	}
	
	public static AttackResult success(float damage) {
		return new AttackResult(ResultType.SUCCESS, damage);
	}
	
	public static AttackResult blocked(float damage) {
		return new AttackResult(ResultType.BLOCKED, damage);
	}
	
	public static AttackResult failed(float damage) {
		return new AttackResult(ResultType.FAILED, damage);
	}
	
	public static enum ResultType {
		SUCCESS(true, true), FAILED(false, false), BLOCKED(false, true);
		
		boolean dealtDamage;
		boolean countMaxStrikes;
		
		ResultType(boolean dealtDamage, boolean count) {
			this.dealtDamage = dealtDamage;
			this.countMaxStrikes = count;
		}
		
		public boolean dealtDamage() {
			return this.dealtDamage;
		}
		
		public boolean shouldCount() {
			return this.countMaxStrikes;
		}
	}
}