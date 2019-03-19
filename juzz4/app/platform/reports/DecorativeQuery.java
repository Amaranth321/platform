package platform.reports;

import java.util.Iterator;
import java.util.List;

import org.bson.types.CodeWScope;

import com.google.code.morphia.Key;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.CriteriaContainerImpl;
import com.google.code.morphia.query.FieldEnd;
import com.google.code.morphia.query.Query;

public abstract class DecorativeQuery<T> implements Query<T> {
	
	private Query<T> rawQuery;
	
	public DecorativeQuery(Query<T> query)
	{
		rawQuery = query;
	}
	
	public Query<T> getRawQuery()
	{
		return rawQuery;
	}

	@Override
	public T get() {
		return getRawQuery().get();
	}

	@Override
	public Key<T> getKey() {
		return getRawQuery().getKey();
	}

	@Override
	public List<T> asList() {
		return getRawQuery().asList();
	}

	@Override
	public List<Key<T>> asKeyList() {
		return getRawQuery().asKeyList();
	}

	@Override
	public Iterable<T> fetch() {
		return getRawQuery().fetch();
	}

	@Override
	public Iterable<T> fetchEmptyEntities() {
		return getRawQuery().fetchEmptyEntities();
	}

	@Override
	public Iterable<Key<T>> fetchKeys() {
		return getRawQuery().fetchKeys();
	}

	@Override
	public long countAll() {
		return getRawQuery().countAll();
	}

	@Override
	public Iterator<T> tail() {
		return getRawQuery().tail();
	}

	@Override
	public Iterator<T> tail(boolean awaitData) {
		return getRawQuery().tail(awaitData);
	}

	@Override
	public Iterator<T> iterator() {
		return getRawQuery().iterator();
	}

	@Override
	public Query<T> filter(String condition, Object value) {
		return getRawQuery().filter(condition, value);
	}

	@Override
	public FieldEnd<? extends Query<T>> field(String field) {
		return getRawQuery().field(field);
	}

	@Override
	public FieldEnd<? extends CriteriaContainerImpl> criteria(String field) {
		return getRawQuery().criteria(field);
	}

	@Override
	public CriteriaContainer and(Criteria... criteria) {
		return getRawQuery().and(criteria);
	}

	@Override
	public CriteriaContainer or(Criteria... criteria) {
		return getRawQuery().or(criteria);
	}

	@Override
	public Query<T> where(String js) {
		return getRawQuery().where(js);
	}

	@Override
	public Query<T> where(CodeWScope js) {
		return getRawQuery().where(js);
	}

	@Override
	public Query<T> order(String condition) {
		return getRawQuery().order(condition);
	}

	@Override
	public Query<T> limit(int value) {
		return getRawQuery().limit(value);
	}

	@Override
	public Query<T> batchSize(int value) {
		return getRawQuery().batchSize(value);
	}

	@Override
	public Query<T> offset(int value) {
		return getRawQuery().offset(value);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Query<T> skip(int value) {
		return getRawQuery().skip(value);
	}

	@Override
	public Query<T> enableValidation() {
		return getRawQuery().enableValidation();
	}

	@Override
	public Query<T> disableValidation() {
		return getRawQuery().disableValidation();
	}

	@Override
	public Query<T> hintIndex(String idxName) {
		return getRawQuery().hintIndex(idxName);
	}

	@Override
	public Query<T> retrievedFields(boolean include, String... fields) {
		return getRawQuery().retrievedFields(include, fields);
	}

	@Override
	public Query<T> enableSnapshotMode() {
		return getRawQuery().enableSnapshotMode();
	}

	@Override
	public Query<T> disableSnapshotMode() {
		return getRawQuery().disableSnapshotMode();
	}

	@Override
	public Query<T> queryNonPrimary() {
		return getRawQuery().queryNonPrimary();
	}

	@Override
	public Query<T> queryPrimaryOnly() {
		return getRawQuery().queryPrimaryOnly();
	}

	@Override
	public Query<T> disableCursorTimeout() {
		return getRawQuery().disableCursorTimeout();
	}

	@Override
	public Query<T> enableCursorTimeout() {
		return getRawQuery().enableCursorTimeout();
	}

	@Override
	public Class<T> getEntityClass() {
		return getRawQuery().getEntityClass();
	}

	@Override
	public Query<T> clone() {
		return getRawQuery().clone();
	}

}
