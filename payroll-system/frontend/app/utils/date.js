import dayjs from 'dayjs'

export function formatDate(date) {
  return dayjs(date).format('YYYY-MM-DD')
}

export function getToday() {
  return dayjs().format('YYYY-MM-DD')
}

export function getCurrentMonthRange() {
  return {
    from: dayjs().startOf('month').format('YYYY-MM-DD'),
    to: dayjs().endOf('month').format('YYYY-MM-DD')
  }
}

export function getCurrentMonth() {
  return dayjs().format('YYYY-MM')
}
